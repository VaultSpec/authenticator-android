package com.vaultspec.authenticator.ui.screen.connect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.imageResource
import com.vaultspec.authenticator.R
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.vaultspec.authenticator.pairing.PairingCode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Seconds since this composable appeared, updated every frame. */
@Composable
fun animationSeconds(): Double {
    var nanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { nanos = it - start }
    }
    return nanos / 1e9
}

/**
 * The pairing cloud, like Apple's Quick Start. With a [token] its specks carry the code (see
 * [PairingCode]); without one it's the same cloud with nothing encoded, for the "connecting" moments.
 * [burstSeconds] (time since the burst started, or null) makes the cloud fly apart and fade.
 */
@Composable
fun ParticleCloud(token: String?, size: Dp, modifier: Modifier = Modifier, burstSeconds: Double? = null) {
    val dark = isSystemInDarkTheme()
    val pixels = with(LocalDensity.current) { size.roundToPx() }
    val code = remember(token, pixels) { PairingCloudArt.render(PairingCode.dots(token.orEmpty()), haze = false, pixels, dark) }
    val glow = ImageBitmap.imageResource(R.drawable.pairing_cloud)
    val t = animationSeconds()
    val burst = burstSeconds?.let { (it / 0.9).coerceIn(0.0, 1.0) } ?: 0.0
    val eased = 1 - (1 - burst).pow(3)

    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = (1 + eased * 0.9).toFloat()
                scaleY = scaleX
                alpha = (1 - burst).toFloat()
            },
    ) {
        // A soft glow for volume, a ball of particles turning in 3D, and the code: a flat layer facing
        // the viewer inside the ball, so a camera always sees it straight on
        val dst = IntSize(this.size.width.toInt(), this.size.height.toInt())
        val breathe = (1 + 0.015 * sin(t * 0.9)).toFloat()
        scale(breathe) { drawImage(glow, dstSize = dst) }
        PairingCloudArt.drawBall(this, t)
        rotate((t * 0.06 * 180 / PI).toFloat()) { drawImage(code, dstSize = dst) }
        PairingCloudArt.drawSparkles(this, dark, t)
    }
}

/**
 * Renders the cloud. Every speck is drawn from the same few blues whether it belongs to the code or to
 * the haze, so colour gives nothing away: the code is only where the specks are denser. A camera, which
 * blurs, sees that density as shapes; the eye sees a soft, grainy sphere. Same layout as the iOS app.
 */
object PairingCloudArt {
    /** R, the reference ring's radius, as a fraction of half the image: inside the artwork's solid body. */
    const val SCALE = 0.5
    /** A speck's size, in R. */
    const val SPECK_SIZE = 0.009
    private const val CODE_DENSITY = 1.15 / (SPECK_SIZE * SPECK_SIZE)
    private const val HAZE_DENSITY = 0.42 / (SPECK_SIZE * SPECK_SIZE)

    /** Speck colours. The same deep blues in dark mode too: the specks sit on the bright glow either way. */
    private val palette = listOf(Color(0.18f, 0.45f, 0.92f, 0.8f), Color(0.3f, 0.58f, 0.97f, 0.8f), Color(0.45f, 0.68f, 1f, 0.8f))

    private class BallPoint(val x: Double, val y: Double, val z: Double, val tone: Int)

    /** Points filling a ball of radius 1.3 (in R), most of them toward its surface so it reads as a sphere. */
    private val ball: List<BallPoint> = Random(41).let { rng ->
        List(6500) { i ->
            val z = rng.nextDouble(-1.0, 1.0)
            val a = rng.nextDouble(0.0, 2 * PI)
            val ring = sqrt(1 - z * z)
            val r = 1.3 * rng.nextDouble(0.02, 1.0).pow(if (i % 4 == 0) 0.33 else 0.08)
            BallPoint(r * ring * cos(a), r * ring * sin(a), r * z, rng.nextInt(3))
        }
    }

    /** Draws the ball turning about a tilted axis, in perspective: nearer particles larger and brighter. */
    fun drawBall(scope: androidx.compose.ui.graphics.drawscope.DrawScope, time: Double) = with(scope) {
        val mid = Offset(size.width / 2, size.height / 2)
        val r = min(size.width, size.height) / 2 * SCALE
        val spin = time * 0.42
        val tilt = 0.38
        val cs = cos(spin); val ss = sin(spin); val ct = cos(tilt); val st = sin(tilt)
        val focal = 3.4
        for (p in ball) {
            val x1 = p.x * cs + p.z * ss
            val z1 = -p.x * ss + p.z * cs
            val y2 = p.y * ct - z1 * st
            val z2 = p.y * st + z1 * ct
            val k = focal / (focal - z2)
            val depth = (z2 + 1.3) / 2.6
            val s = max(0.8, r * 0.009 * k * (0.7 + 0.6 * depth)).toFloat()
            val alpha = floatArrayOf(0.35f, 0.6f, 0.9f)[min(2, (depth * 3).toInt())]
            drawRect(
                palette[p.tone].copy(alpha = alpha * 0.8f),
                topLeft = Offset((mid.x + r * x1 * k - s / 2).toFloat(), (mid.y + r * y2 * k - s / 2).toFloat()),
                size = Size(s, s),
            )
        }
    }

    /** Either the code's specks ([dots]) or the haze, on a transparent image [pixels] wide. */
    fun render(dots: List<PairingCode.Dot>?, haze: Boolean, pixels: Int, dark: Boolean): ImageBitmap {
        val image = ImageBitmap(pixels, pixels)
        val canvas = androidx.compose.ui.graphics.Canvas(image)
        val paints = palette.map { Paint().apply { color = it } }
        val half = pixels / 2.0
        val r = half * SCALE
        val s = max(1.0, r * SPECK_SIZE)
        val rng = Random(if (haze) 31 else 17)

        fun speck(x: Double, y: Double) {
            val left = (half + r * x - s / 2).toFloat()
            val top = (half + r * y - s / 2).toFloat()
            canvas.drawRect(left, top, left + s.toFloat(), top + s.toFloat(), paints[rng.nextInt(paints.size)])
        }
        fun fillDisk(cx: Double, cy: Double, radius: Double, density: Double) {
            repeat((PI * radius * radius * density).toInt()) {
                val d = radius * sqrt(rng.nextDouble())
                val a = rng.nextDouble(0.0, 2 * PI)
                speck(cx + d * cos(a), cy + d * sin(a))
            }
        }

        if (haze) {
            // Even through the middle, thinning past the ring into a soft edge
            val outer = 1.45
            repeat((PI * outer * outer * HAZE_DENSITY).toInt()) {
                val d = outer * sqrt(rng.nextDouble())
                val a = rng.nextDouble(0.0, 2 * PI)
                if (rng.nextDouble() < exp(-(max(0.0, d - 0.8) / 0.3).pow(2))) speck(d * cos(a), d * sin(a))
            }
        }
        if (dots != null) {
            fillDisk(0.0, 0.0, PairingCode.CENTER_RADIUS, CODE_DENSITY)
            var made = 0
            val ringCount = (2 * PI * PairingCode.RING_WIDTH * CODE_DENSITY).toInt()
            while (made < ringCount) {
                val a = rng.nextDouble(0.0, 2 * PI)
                if (abs(Math.IEEEremainder(a, 2 * PI)) <= PairingCode.GAP / 2) continue
                val d = 1 + PairingCode.RING_WIDTH * rng.nextDouble(-0.5, 0.5)
                speck(d * cos(a), d * sin(a))
                made++
            }
            for (dot in dots) if (dot.on) {
                fillDisk(dot.radius * cos(dot.angle), dot.radius * sin(dot.angle), PairingCode.DOT_DIAMETER / 2, CODE_DENSITY)
            }
        }
        return image
    }

    /** A few hundred specks that twinkle over the cloud, so it shimmers. */
    fun drawSparkles(scope: androidx.compose.ui.graphics.drawscope.DrawScope, dark: Boolean, time: Double) = with(scope) {
        val mid = Offset(size.width / 2, size.height / 2)
        val r = min(size.width, size.height) / 2 * SCALE
        val s = max(1.0, r * SPECK_SIZE * 1.4).toFloat()
        val color = if (dark) Color(0.9f, 0.95f, 1f) else Color(0.2f, 0.5f, 0.98f)
        val rng = Random(5)
        repeat(360) {
            val d = 1.35 * sqrt(rng.nextDouble())
            val a = rng.nextDouble(0.0, 2 * PI) + time * rng.nextDouble(-0.12, 0.12)
            val phase = rng.nextDouble(0.0, 2 * PI)
            val alpha = if (sin(time * 2.4 + phase) > 0.3) 0.75f else 0.25f
            drawRect(
                color.copy(alpha = alpha),
                topLeft = Offset((mid.x + r * d * cos(a) - s / 2).toFloat(), (mid.y + r * d * sin(a) - s / 2).toFloat()),
                size = Size(s, s),
            )
        }
    }
}
