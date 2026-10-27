package com.vaultspec.authenticator.pairing

import java.util.Base64
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The particle code shown by Scan & Connect: a 12-byte pairing token drawn as dots on rings, read back
 * from camera frames by [decode]. The iOS app implements the same format (PairingCode.swift).
 *
 * Geometry, in units of R (the radius of the reference ring), angles clockwise in image coordinates:
 * - a bright disk at the centre, radius 0.2 (finds the centre and the scale)
 * - the reference ring at radius 1, with a 24° gap centred on the orientation angle φ
 * - 112 data positions on four rings (radii 0.42, 0.57, 0.72, 0.87; 16, 24, 32 and 40 positions).
 *   Position j of ring k sits at φ + 2π(j + 0.5)/count. A dot means 1.
 * - the bits are the token followed by its CRC-16/CCITT-FALSE, most significant bit first.
 */
object PairingCode {
    const val TOKEN_LENGTH = 12
    const val CENTER_RADIUS = 0.2
    const val RING_WIDTH = 0.05
    const val GAP = 24.0 * PI / 180
    const val DOT_DIAMETER = 0.085
    val rings = listOf(0.42 to 16, 0.57 to 24, 0.72 to 32, 0.87 to 40)
    val bitCount = rings.sumOf { it.second }

    /** One data position, relative to the centre (unit R, before rotation by φ), and whether it shows a dot. */
    data class Dot(val radius: Double, val angle: Double, val on: Boolean)

    /** The dots for a token given as base64url text, or null if it isn't a 12-byte token. */
    fun dots(token: String): List<Dot>? {
        val bytes = runCatching { Base64.getUrlDecoder().decode(token) }.getOrNull() ?: return null
        if (bytes.size != TOKEN_LENGTH) return null
        val bits = bitsOf(bytes + crc16(bytes))
        var index = 0
        return rings.flatMap { (radius, count) ->
            (0 until count).map { j -> Dot(radius, 2 * PI * (j + 0.5) / count, bits[index++]) }
        }
    }

    // ── Decoding ──

    /**
     * Reads a code from an 8-bit luma plane (a camera frame's Y plane), light on dark or dark on light, assuming it is roughly in the
     * middle and facing the camera. Returns the token as base64url, or null if no valid code was found.
     */
    fun decode(luma: ByteArray, width: Int, height: Int, rowStride: Int): String? =
        decode(Gray.downsample(luma, width, height, rowStride, target = 360))

    /** Tries the frame as it is (a light code on a dark screen) and inverted (a dark cloud on a light screen). */
    fun decode(g: Gray): String? =
        decodeLight(g) ?: decodeLight(Gray(g.width, g.height, DoubleArray(g.pixels.size) { 1 - g.pixels[it] }))

    private fun decodeLight(frame: Gray): String? {
        // Merge the cloud's specks into solid shapes, as a slightly soft camera would
        val soft = frame.boxBlurred(max(1, min(frame.width, frame.height) / 180))
        // Keep only detail smaller than the code's centre: smooth shading (the artwork behind the code,
        // glare, vignetting) drops out
        val broad = soft.boxBlurred(max(4, min(frame.width, frame.height) / 18))
        val g = Gray(soft.width, soft.height, DoubleArray(soft.pixels.size) { (0.5 + 2 * (soft.pixels[it] - broad.pixels[it])).coerceIn(0.0, 1.0) })
        val minSide = min(g.width, g.height).toDouble()
        val blurred = g.boxBlurred(max(2, (minSide * 0.02).toInt()))

        // The centre disk is the brightest large blob near the middle of the frame
        val search = (minSide * 0.3).toInt()
        var bx = g.width / 2
        var by = g.height / 2
        var bv = -1.0
        for (y in max(0, g.height / 2 - search) until min(g.height, g.height / 2 + search)) {
            for (x in max(0, g.width / 2 - search) until min(g.width, g.width / 2 + search)) {
                if (blurred[x, y] > bv) { bv = blurred[x, y]; bx = x; by = y }
            }
        }
        val background = g.percentile(0.2, bx, by, search)
        val peak = g.maxValue(bx, by, 3)
        if (peak - background <= 0.12) return null
        val threshold = (peak + background) / 2

        val disk = g.blob(bx, by, threshold, (minSide * 0.2).toInt()) ?: return null
        val r0 = sqrt(disk.area / PI)
        if (r0 < 3) return null

        // Refine the centre and the scale on the reference ring
        var cx = disk.cx
        var cy = disk.cy
        var radius = r0 / CENTER_RADIUS
        var bestScore = -1.0
        val c0x = cx
        val c0y = cy
        val rEstimate = radius
        for (dy in -2..2) for (dx in -2..2) {
            var scale = 0.85
            while (scale <= 1.1500001) {
                val x = c0x + dx
                val y = c0y + dy
                val r = rEstimate * scale
                var sum = 0.0
                for (i in 0 until 120) {
                    val a = i / 120.0 * 2 * PI
                    sum += g.sample(x + r * cos(a), y + r * sin(a))
                }
                if (sum > bestScore) { bestScore = sum; cx = x; cy = y; radius = r }
                scale += 0.01
            }
        }
        if (radius >= minSide * 0.6) return null

        // Orientation: the gap in the reference ring
        val steps = 360
        val ring = DoubleArray(steps) { i ->
            val a = i.toDouble() / steps * 2 * PI
            doubleArrayOf(0.96, 1.0, 1.04).maxOf { g.sample(cx + radius * it * cos(a), cy + radius * it * sin(a)) }
        }
        val window = 20
        var gapStart = 0
        var gapSum = Double.MAX_VALUE
        for (start in 0 until steps) {
            var sum = 0.0
            for (k in 0 until window) sum += ring[(start + k) % steps]
            if (sum < gapSum) { gapSum = sum; gapStart = start }
        }
        val phi = (gapStart + (window - 1) / 2.0) / steps * 2 * PI

        // Fine-tune the rotation until the dots read most clearly, then read them
        var bestPhi = phi
        var bestClarity = -1.0
        for (step in -8..8) {
            val candidate = phi + step * 0.5 * PI / 180
            val clarity = sampleBits(g, cx, cy, radius, candidate).sumOf { abs(it - 0.5) }
            if (clarity > bestClarity) { bestClarity = clarity; bestPhi = candidate }
        }
        val values = sampleBits(g, cx, cy, radius, bestPhi)
        // Each ring gets its own threshold, so shading that differs from ring to ring doesn't matter
        val thresholds = DoubleArray(values.size)
        var start = 0
        for ((_, count) in rings) {
            val t = otsu(values.copyOfRange(start, start + count))
            for (i in start until start + count) thresholds[i] = t
            start += count
        }
        return readBits(values, thresholds)
    }

    /** The mean brightness at every data position. */
    private fun sampleBits(g: Gray, cx: Double, cy: Double, radius: Double, phi: Double): DoubleArray {
        val values = DoubleArray(bitCount)
        var index = 0
        for ((ringRadius, count) in rings) {
            val step = 2 * PI / count
            for (j in 0 until count) {
                val a = phi + step * (j + 0.5)
                var sum = 0.0
                for (dr in doubleArrayOf(-0.015, 0.0, 0.015)) for (da in doubleArrayOf(-0.1, 0.0, 0.1)) {
                    val r = radius * (ringRadius + dr)
                    val angle = a + da * step
                    sum += g.sample(cx + r * cos(angle), cy + r * sin(angle))
                }
                values[index++] = sum / 9
            }
        }
        return values
    }

    /** The cut that best separates [values] into two groups (Otsu's method). */
    private fun otsu(values: DoubleArray): Double {
        val sorted = values.sortedArray()
        var best = (sorted.first() + sorted.last()) / 2
        var bestScore = -1.0
        for (k in 1 until sorted.size) {
            val m0 = sorted.copyOfRange(0, k).average()
            val m1 = sorted.copyOfRange(k, sorted.size).average()
            val score = k.toDouble() * (sorted.size - k) * (m1 - m0) * (m1 - m0)
            if (score > bestScore) { bestScore = score; best = (sorted[k - 1] + sorted[k]) / 2 }
        }
        return best
    }

    /**
     * Thresholds the dots and checks the CRC. If it fails, retries with the least certain bits flipped
     * (up to 6 of them), which recovers a few misread dots; the CRC still rejects anything wrong.
     */
    private fun readBits(values: DoubleArray, thresholds: DoubleArray): String? {
        val bits = BooleanArray(values.size) { values[it] > thresholds[it] }
        val doubtful = values.indices.sortedBy { abs(values[it] - thresholds[it]) }.take(6)
        for (mask in 0 until (1 shl doubtful.size)) {
            val attempt = bits.copyOf()
            doubtful.forEachIndexed { k, index -> if (mask and (1 shl k) != 0) attempt[index] = !attempt[index] }
            val bytes = bytesOf(attempt.toList())
            val token = bytes.copyOfRange(0, TOKEN_LENGTH)
            if (bytes.copyOfRange(TOKEN_LENGTH, TOKEN_LENGTH + 2).contentEquals(crc16(token))) {
                return Base64.getUrlEncoder().withoutPadding().encodeToString(token)
            }
        }
        return null
    }

    // ── Bits and bytes ──

    fun crc16(bytes: ByteArray): ByteArray {
        var crc = 0xFFFF
        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) { crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) and 0xFFFF else (crc shl 1) and 0xFFFF }
        }
        return byteArrayOf((crc shr 8).toByte(), crc.toByte())
    }

    private fun bitsOf(bytes: ByteArray): List<Boolean> =
        bytes.flatMap { b -> (0 until 8).map { (b.toInt() and (0x80 shr it)) != 0 } }

    private fun bytesOf(bits: List<Boolean>): ByteArray = ByteArray(bits.size / 8) { i ->
        var v = 0
        for (k in 0 until 8) if (bits[i * 8 + k]) v = v or (0x80 shr k)
        v.toByte()
    }

    // ── Image ──

    /** A small grayscale image, values 0..1. */
    class Gray(val width: Int, val height: Int, val pixels: DoubleArray) {
        operator fun get(x: Int, y: Int) = pixels[y * width + x]

        /** Bilinear sample; 0 outside the image. */
        fun sample(x: Double, y: Double): Double {
            if (x < 0 || y < 0 || x >= width - 1 || y >= height - 1) return 0.0
            val x0 = x.toInt()
            val y0 = y.toInt()
            val fx = x - x0
            val fy = y - y0
            val top = this[x0, y0] * (1 - fx) + this[x0 + 1, y0] * fx
            val bottom = this[x0, y0 + 1] * (1 - fx) + this[x0 + 1, y0 + 1] * fx
            return top * (1 - fy) + bottom * fy
        }

        fun boxBlurred(radius: Int): Gray {
            val stride = width + 1
            val integral = DoubleArray(stride * (height + 1))
            for (y in 0 until height) {
                var row = 0.0
                for (x in 0 until width) {
                    row += this[x, y]
                    integral[(y + 1) * stride + x + 1] = integral[y * stride + x + 1] + row
                }
            }
            val out = DoubleArray(width * height)
            for (y in 0 until height) {
                val y0 = max(0, y - radius)
                val y1 = min(height, y + radius + 1)
                for (x in 0 until width) {
                    val x0 = max(0, x - radius)
                    val x1 = min(width, x + radius + 1)
                    val sum = integral[y1 * stride + x1] - integral[y0 * stride + x1] - integral[y1 * stride + x0] + integral[y0 * stride + x0]
                    out[y * width + x] = sum / ((x1 - x0) * (y1 - y0))
                }
            }
            return Gray(width, height, out)
        }

        fun percentile(p: Double, cx: Int, cy: Int, radius: Int): Double {
            val values = ArrayList<Double>()
            for (y in max(0, cy - radius) until min(height, cy + radius) step 2) {
                for (x in max(0, cx - radius) until min(width, cx + radius) step 2) values.add(this[x, y])
            }
            values.sort()
            return if (values.isEmpty()) 0.0 else values[((values.size - 1) * p).toInt()]
        }

        fun maxValue(cx: Int, cy: Int, radius: Int): Double {
            var best = 0.0
            for (y in max(0, cy - radius)..min(height - 1, cy + radius)) {
                for (x in max(0, cx - radius)..min(width - 1, cx + radius)) best = max(best, this[x, y])
            }
            return best
        }

        class Blob(val area: Int, val cx: Double, val cy: Double)

        /** The connected bright region containing (sx, sy): its pixel count and centroid. */
        fun blob(sx: Int, sy: Int, threshold: Double, maxRadius: Int): Blob? {
            if (this[sx, sy] <= threshold) return null
            val seen = BooleanArray(width * height)
            val stack = ArrayDeque<Int>()
            stack.addLast(sy * width + sx)
            seen[sy * width + sx] = true
            var area = 0
            var sumX = 0L
            var sumY = 0L
            while (stack.isNotEmpty()) {
                val i = stack.removeLast()
                val x = i % width
                val y = i / width
                area++; sumX += x; sumY += y
                if (abs(x - sx) >= maxRadius || abs(y - sy) >= maxRadius) return null
                for ((nx, ny) in arrayOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue
                    val n = ny * width + nx
                    if (!seen[n] && this[nx, ny] > threshold) { seen[n] = true; stack.addLast(n) }
                }
            }
            return Blob(area, sumX.toDouble() / area, sumY.toDouble() / area)
        }

        companion object {
            /** Averages blocks so the shorter side is about [target] pixels. */
            fun downsample(luma: ByteArray, width: Int, height: Int, rowStride: Int, target: Int): Gray {
                val f = max(1, min(width, height) / target)
                val w = width / f
                val h = height / f
                val scale = 1.0 / (255.0 * f * f)
                val pixels = DoubleArray(w * h)
                for (y in 0 until h) for (x in 0 until w) {
                    var sum = 0
                    for (yy in 0 until f) {
                        val row = (y * f + yy) * rowStride + x * f
                        for (xx in 0 until f) sum += luma[row + xx].toInt() and 0xFF
                    }
                    pixels[y * w + x] = sum * scale
                }
                return Gray(w, h, pixels)
            }
        }
    }
}
