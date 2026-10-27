package com.vaultspec.authenticator.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class PairingCodeTest {
    private val token = "q1w2e3r4t5y6u7i8"

    /** Luma of an image, cropped to a square of [side] pixels centred at (cx, cy). */
    private fun luma(image: BufferedImage, cx: Int, cy: Int, side: Int): PairingCode.Gray {
        val pixels = DoubleArray(side * side) { i ->
            val rgb = image.getRGB(cx - side / 2 + i % side, cy - side / 2 + i / side)
            (0.299 * (rgb shr 16 and 0xFF) + 0.587 * (rgb shr 8 and 0xFF) + 0.114 * (rgb and 0xFF)) / 255
        }
        return PairingCode.Gray(side, side, pixels)
    }

    /** Draws the code from the format description alone, as an independent check of the geometry. */
    private fun render(size: Int, radius: Double, rotation: Double, noise: Double): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(8, 14, 40)
        g.fillRect(0, 0, size, size)
        g.color = Color.WHITE
        val c = size / 2.0
        fun dot(r: Double, a: Double, d: Double) =
            g.fill(Ellipse2D.Double(c + radius * r * cos(a) - d / 2, c + radius * r * sin(a) - d / 2, d, d))
        dot(0.0, 0.0, radius * PairingCode.CENTER_RADIUS * 2)
        for (i in 0 until 150) {
            val a = i / 150.0 * 2 * PI
            if (abs(Math.IEEEremainder(a, 2 * PI)) > PairingCode.GAP / 2) dot(1.0, a + rotation, radius * PairingCode.RING_WIDTH)
        }
        PairingCode.dots(token)!!.filter { it.on }.forEach { dot(it.radius, it.angle + rotation, radius * PairingCode.DOT_DIAMETER) }
        val random = Random(1)
        for (y in 0 until size) for (x in 0 until size) {
            val v = image.getRGB(x, y) and 0xFF
            val n = (v + random.nextDouble(-noise, noise) * 255).toInt().coerceIn(0, 255)
            image.setRGB(x, y, Color(n, n, n).rgb)
        }
        return image
    }

    @Test fun decodesTheIphoneScreen() {
        val image = ImageIO.read(javaClass.getResource("/pairing/ios-screen.png"))
        // The code sits at y ≈ 1000 of the 1206×2622 screenshot; a camera would frame about this much
        assertEquals(token, PairingCode.decode(luma(image, image.width / 2, 950, 1100)))
    }

    @Test fun decodesRotatedNoisyCodes() {
        for ((i, rotation) in listOf(0.0, 1.1, 2.9, 4.4, 6.0).withIndex()) {
            val image = render(420, 90.0 + i * 12, rotation, 0.06)
            assertEquals("rotation $rotation", token, PairingCode.decode(luma(image, 210 + i * 3, 210 - i * 2, 380)))
        }
    }

    @Test fun rejectsNoise() {
        val random = Random(2)
        assertNull(PairingCode.decode(PairingCode.Gray(360, 360, DoubleArray(360 * 360) { random.nextDouble(0.0, 0.3) })))
    }

    @Test fun rejectsBadTokens() {
        assertNull(PairingCode.dots("short"))
        assertEquals(PairingCode.bitCount, PairingCode.dots(token)!!.size)
    }
}
