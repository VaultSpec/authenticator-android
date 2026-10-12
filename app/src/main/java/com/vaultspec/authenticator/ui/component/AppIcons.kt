package com.vaultspec.authenticator.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's icon set: outline glyphs from Lucide 1.54.0 (https://lucide.dev, ISC license),
 * drawn on a 24×24 grid with a 2-unit round stroke. Use these instead of Material icons so
 * every screen shares one icon style.
 */
object AppIcons {
    private fun stroked(vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

    /** lucide: plus */
    val Plus: ImageVector by lazy {
        stroked(
        "M5 12h14",
        "M12 5v14",
        )
    }

    /** lucide: chevron-left */
    val ChevronLeft: ImageVector by lazy {
        stroked(
        "m15 18-6-6 6-6",
        )
    }

    /** lucide: chevron-right */
    val ChevronRight: ImageVector by lazy {
        stroked(
        "m9 18 6-6-6-6",
        )
    }

    /** lucide: chevrons-up-down */
    val ChevronsUpDown: ImageVector by lazy {
        stroked(
        "m7 15 5 5 5-5",
        "m7 9 5-5 5 5",
        )
    }

    /** lucide: cloud-upload */
    val CloudUpload: ImageVector by lazy {
        stroked(
        "M12 13v8",
        "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242",
        "m8 17 4-4 4 4",
        )
    }

    /** lucide: camera */
    val Camera: ImageVector by lazy {
        stroked(
        "M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z",
        "M9 13a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        )
    }

    /** lucide: circle-x */
    val CircleX: ImageVector by lazy {
        stroked(
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
        "m15 9-6 6",
        "m9 9 6 6",
        )
    }

    /** lucide: check */
    val Check: ImageVector by lazy {
        stroked(
        "M20 6 9 17l-5-5",
        )
    }

    /** lucide: circle-check */
    val CircleCheck: ImageVector by lazy {
        stroked(
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
        "m16 9-5.5 5.5L8 12",
        )
    }

    /** lucide: copy */
    val Copy: ImageVector by lazy {
        stroked(
        "M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2Z",
        "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2",
        )
    }

    /** lucide: contrast */
    val Contrast: ImageVector by lazy {
        stroked(
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
        "M12 18a6 6 0 0 0 0-12v12z",
        )
    }

    /** lucide: moon */
    val Moon: ImageVector by lazy {
        stroked(
        "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401",
        )
    }

    /** lucide: trash-2 */
    val Trash: ImageVector by lazy {
        stroked(
        "M10 11v6",
        "M14 11v6",
        "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
        "M3 6h18",
        "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
        )
    }

    /** lucide: file-up */
    val FileUp: ImageVector by lazy {
        stroked(
        "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z",
        "M14 2v5a1 1 0 0 0 1 1h5",
        "M12 12v6",
        "m15 15-3-3-3 3",
        )
    }

    /** lucide: list-filter */
    val ListFilter: ImageVector by lazy {
        stroked(
        "M2 5h20",
        "M6 12h12",
        "M9 19h6",
        )
    }

    /** lucide: fingerprint */
    val Fingerprint: ImageVector by lazy {
        stroked(
        "M12 10a2 2 0 0 0-2 2c0 1.02-.1 2.51-.26 4",
        "M14 13.12c0 2.38 0 6.38-1 8.88",
        "M17.29 21.02c.12-.6.43-2.3.5-3.02",
        "M2 12a10 10 0 0 1 18-6",
        "M2 16h.01",
        "M21.8 16c.2-2 .131-5.354 0-6",
        "M5 19.5C5.5 18 6 15 6 12a6 6 0 0 1 .34-2",
        "M8.65 22c.21-.66.45-1.32.57-2",
        "M9 6.8a6 6 0 0 1 9 5.2v2",
        )
    }

    /** lucide: folder */
    val Folder: ImageVector by lazy {
        stroked(
        "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z",
        )
    }

    /** lucide: highlighter */
    val Highlighter: ImageVector by lazy {
        stroked(
        "m9 11-6 6v3h9l3-3",
        "m22 12-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4",
        )
    }

    /** lucide: info */
    val Info: ImageVector by lazy {
        stroked(
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
        "M12 16v-4",
        "M12 8h.01",
        )
    }

    /** lucide: key-round */
    val Key: ImageVector by lazy {
        stroked(
        "M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z",
        "M16 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0",
        )
    }

    /** lucide: lock */
    val Lock: ImageVector by lazy {
        stroked(
        "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-7a2 2 0 0 1 2 -2Z",
        "M7 11V7a5 5 0 0 1 10 0v4",
        )
    }

    /** lucide: lock-keyhole */
    val LockKeyhole: ImageVector by lazy {
        stroked(
        "M11 16a1 1 0 1 0 2 0a1 1 0 1 0 -2 0",
        "M5 10h14a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2Z",
        "M7 10V7a5 5 0 0 1 10 0v3",
        )
    }

    /** lucide: shield-check */
    val ShieldCheck: ImageVector by lazy {
        stroked(
        "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        "m9 12 2 2 4-4",
        )
    }

    /** lucide: scan-qr-code */
    val ScanQrCode: ImageVector by lazy {
        stroked(
        "M17 12v4a1 1 0 0 1-1 1h-4",
        "M17 3h2a2 2 0 0 1 2 2v2",
        "M17 8V7",
        "M21 17v2a2 2 0 0 1-2 2h-2",
        "M3 7V5a2 2 0 0 1 2-2h2",
        "M7 17h.01",
        "M7 21H5a2 2 0 0 1-2-2v-2",
        "M8 7h3a1 1 0 0 1 1 1v3a1 1 0 0 1 -1 1h-3a1 1 0 0 1 -1 -1v-3a1 1 0 0 1 1 -1Z",
        )
    }

    /** lucide: history */
    val History: ImageVector by lazy {
        stroked(
        "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
        "M3 3v5h5",
        "M12 7v5l4 2",
        )
    }

    /** lucide: calendar-clock */
    val CalendarClock: ImageVector by lazy {
        stroked(
        "M16 14v2.2l1.6 1",
        "M16 2v3",
        "M21 7.338V5a2 2 0 00-2-2H5a2 2 0 00-2 2v14a2 2 0 002 2h2.338",
        "M3 9h5.859",
        "M8 2v3",
        "M10 16a6 6 0 1 0 12 0a6 6 0 1 0 -12 0",
        )
    }

    /** lucide: smartphone */
    val Smartphone: ImageVector by lazy {
        stroked(
        "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2Z",
        "M12 18h.01",
        )
    }

    /** lucide: search */
    val Search: ImageVector by lazy {
        stroked(
        "m21 21-4.34-4.34",
        "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0",
        )
    }

    /** lucide: search-x */
    val SearchX: ImageVector by lazy {
        stroked(
        "m13.5 8.5-5 5",
        "m8.5 8.5 5 5",
        "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0",
        "m21 21-4.3-4.3",
        )
    }

    /** lucide: settings */
    val Settings: ImageVector by lazy {
        stroked(
        "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        )
    }

    /** lucide: shield */
    val Shield: ImageVector by lazy {
        stroked(
        "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        )
    }

    /** lucide: refresh-cw */
    val RefreshCw: ImageVector by lazy {
        stroked(
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
        "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
        "M8 16H3v5",
        )
    }

    /** lucide: timer */
    val Timer: ImageVector by lazy {
        stroked(
        "M10 2L14 2",
        "M12 14L15 11",
        "M4 14a8 8 0 1 0 16 0a8 8 0 1 0 -16 0",
        )
    }

    /** lucide: eye */
    val Eye: ImageVector by lazy {
        stroked(
        "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        )
    }

    /** lucide: eye-off */
    val EyeOff: ImageVector by lazy {
        stroked(
        "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49",
        "M14.084 14.158a3 3 0 0 1-4.242-4.242",
        "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143",
        "m2 2 20 20",
        )
    }

    /** lucide: cloud */
    val Cloud: ImageVector by lazy {
        stroked(
        "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z",
        )
    }

    /** lucide: cloud-download */
    val CloudDownload: ImageVector by lazy {
        stroked(
        "M12 13v8l-4-4",
        "m12 21 4-4",
        "M4.393 15.269A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.436 8.284",
        )
    }

    /** lucide: circle-user-round */
    val User: ImageVector by lazy {
        stroked(
        "M17.925 20.056a6 6 0 0 0-11.851.001",
        "M8 11a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0",
        )
    }

    /** lucide: log-out */
    val LogOut: ImageVector by lazy {
        stroked(
        "m16 17 5-5-5-5",
        "M21 12H9",
        "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4",
        )
    }

    /** lucide: user-x */
    val UserX: ImageVector by lazy {
        stroked(
        "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
        "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
        "M17 8L22 13",
        "M22 8L17 13",
        )
    }

    /** lucide: mail */
    val Mail: ImageVector by lazy {
        stroked(
        "m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7",
        "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2Z",
        )
    }
}
