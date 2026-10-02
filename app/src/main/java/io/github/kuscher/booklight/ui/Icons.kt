package io.github.kuscher.booklight.ui

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.kuscher.booklight.core.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Booklight's own symbols (24 dp, one path each), drawn in the row's colour. */
object Symbols {
    private fun icon(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(path), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd).build()

    val search = icon("search", "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z")
    val settings = icon("settings", "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z")
    val globe = icon("globe", "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z")
    val calc = icon("calc", "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5.97 4.06L14.09 6l1.41 1.41L16.91 6l1.06 1.06-1.41 1.41 1.41 1.41-1.06 1.06-1.41-1.4-1.41 1.41-1.06-1.06 1.41-1.41-1.41-1.42zm-6.78.66h5v1.5h-5v-1.5zM11.5 16h-2v2H8v-2H6v-1.5h2v-2h1.5v2h2V16zm6.5 1.25h-5v-1.5h5v1.5zm0-2.5h-5v-1.5h5v1.5z")
    val enter = icon("enter", "M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7z")
    /** Booklight's mark: a lamp head with its beam. */
    val booklight = icon("booklight", "M6.75 2.8 L17.25 2.8 A3.25 3.25 0 0 1 17.25 9.3 L6.75 9.3 A3.25 3.25 0 0 1 6.75 2.8 Z M7.2 4.5 A0.5 0.5 0 0 1 7.7 4 L8.3 4 A0.5 0.5 0 0 1 8.8 4.5 L8.8 7.6 A0.5 0.5 0 0 1 8.3 8.1 L7.7 8.1 A0.5 0.5 0 0 1 7.2 7.6 Z M6.55 12.77 A1.75 1.75 0 0 1 8.16 11.7 L15.84 11.7 A1.75 1.75 0 0 1 17.45 12.77 L19.76 18.25 A2.12 2.12 0 0 1 17.8 21.2 L6.2 21.2 A2.12 2.12 0 0 1 4.24 18.25 Z")
    val app = icon("app", "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z")
    val check = icon("check", "M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z")

    /** A window's outline: the frame every place symbol is cut out of. */
    private const val WINDOW = "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"

    // What a row can do: one icon per action.
    private val more = mapOf(
        "open" to "M14 3h7v7h-2V6.41l-8.3 8.3-1.4-1.42L17.58 5H14zM5 5h6v2H5v12h12v-6h2v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
        "window" to "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm0 4v10h16V8zm7 2h2v2h2v2h-2v2h-2v-2H9v-2h2z",
        "info" to "M11 7h2v2h-2zm0 4h2v6h-2zm1-9a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16z",
        "left" to "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm8 2v12h8V6z",
        "right" to "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm0 2v12h8V6z",
        "store" to "M6 7V6a6 6 0 0 1 12 0v1h3v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zm2 0h8V6a4 4 0 0 0-8 0zM5 9v11h14V9zM8 11h2a2 2 0 0 0 4 0h2a4 4 0 0 1-8 0z",
        "trash" to "M9 3h6l1 1h4v2H4V4h4zM6 8h12l-1 12a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2zm3.5 2.5V19H11v-8.5zm3.5 0V19h1.5v-8.5z",
        "copy" to "M8 3h10a2 2 0 0 1 2 2v12h-2V5H8zM5 7h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zm0 2v10h10V9z",
        "edit" to "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75z",
        "share" to "M18 16a3 3 0 0 0-2.4 1.2L8.9 13.3a3 3 0 0 0 0-2.6l6.7-3.9A3 3 0 1 0 15 5q0 .4.1.7L8.4 9.6a3 3 0 1 0 0 4.8l6.700 3.900q-.1.3-.1.7a3 3 0 1 0 3-3z",
        "save" to "M5 20h14v-2H5zM19 9h-4V3H9v6H5l7 7z",
        "again" to "M17.65 6.35A8 8 0 1 0 19.73 14h-2.08A6 6 0 1 1 12 6c1.66 0 3.14.69 4.22 1.78L13 11h7V4z",
        "link" to "M3.9 12a3.1 3.1 0 0 1 3.1-3.1h4V7H7a5 5 0 0 0 0 10h4v-1.900H7A3.1 3.1 0 0 1 3.9 12zM8 13h8v-2H8zm9-6h-4v1.900h4a3.1 3.1 0 0 1 0 6.200h-4V17h4a5 5 0 0 0 0-10z",
        "play" to "M8 5v14l11-7z",
        "pause" to "M6 5h4v14H6zm8 0h4v14h-4z",
        "stop" to "M6 6h12v12H6z",
        "next" to "M6 18l8.5-6L6 6zM16 6h2v12h-2z",
        "previous" to "M6 6h2v12H6zm3.5 6 8.5 6V6z",
        "labs" to "M19.8 18.4 14 10.67V6.5l1.35-1.69c.26-.33.03-.81-.39-.81H9.04c-.42 0-.65.48-.39.81L10 6.5v4.17L4.2 18.4c-.49.66-.02 1.6.8 1.6h14c.82 0 1.29-.94.8-1.6z",
        "lock" to "M12 2a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1V7a5 5 0 0 0-5-5zm-3 8V7a3 3 0 0 1 6 0v3zM6 12h12v8H6zm6 2.5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z",
        // A link without its tracking tail: the link, and the spark of something tidied.
        "clean" to "M3.9 13a3.1 3.1 0 0 1 3.1-3.1h4V8H7a5 5 0 0 0 0 10h4v-1.9H7A3.1 3.1 0 0 1 3.9 13zM8 14h8v-2H8zm9-6h-4v1.9h4a3.1 3.1 0 0 1 0 6.2h-4V18h4a5 5 0 0 0 0-10zM19 .5l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8z",
        "phone" to "M6.62 10.79a15.15 15.15 0 0 0 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1C10.61 21 3 13.39 3 4c0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02z",
        "mail" to "M20 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 4-8 5-8-5V6l8 5 8-5z",
        "note" to "M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h9l7-7V5a2 2 0 0 0-2-2zM7 8h10v2H7zm0 4h5v2H7zm7 7.500V14h5.500z",
        "timer" to "M15 1H9v2h6zm-4 13h2V8h-2zm8.030-6.610 1.420-1.420a11 11 0 0 0-1.410-1.410l-1.420 1.420A9 9 0 1 0 19.030 7.390zM12 20a7 7 0 1 1 0-14 7 7 0 0 1 0 14z",
        "event" to "M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 16H5V10h14zM7 12h5v5H7z",
        "plus" to "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z",
        "spark" to "M12 2l1.900 6.100L20 10l-6.100 1.900L12 18l-1.900-6.100L4 10l6.100-1.900zM19 15l.900 2.600 2.600.900-2.600.900L19 22l-.900-2.600-2.600-.900 2.600-.900z",
        "smile" to "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm3.500 6a1.500 1.500 0 1 1 0 3 1.500 1.500 0 0 1 0-3zm-7 0a1.500 1.500 0 1 1 0 3 1.500 1.500 0 0 1 0-3zM12 17.500c-2.330 0-4.310-1.460-5.110-3.500h10.220c-.800 2.040-2.780 3.500-5.110 3.500z",
        "qr" to "M3 11h8V3H3zm2-6h4v4H5zM3 21h8v-8H3zm2-6h4v4H5zm8-12v8h8V3zm6 6h-4V5h4zm-6 4h2v2h-2zm2 2h2v2h-2zm2-2h2v2h-2zm0 4h2v2h-2zm-4 0h2v2h-2zm2 2h2v2h-2zm2-2h2v2h-2z",
        "volume" to "M3 9v6h4l5 5V4L7 9zm13.500 3A4.500 4.500 0 0 0 14 8v8a4.500 4.500 0 0 0 2.500-4zM14 3.200v2.100a7 7 0 0 1 0 13.400v2.100a9 9 0 0 0 0-17.600z",
        "mute" to "M3 9v6h4l5 5V4L7 9zm18.500.900-1.400-1.400L18 10.600l-2.100-2.100-1.400 1.400 2.100 2.100-2.100 2.100 1.400 1.400 2.100-2.100 2.100 2.100 1.400-1.400-2.100-2.100z",
        "sun" to "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM2 13h2a1 1 0 0 0 0-2H2a1 1 0 0 0 0 2zm18 0h2a1 1 0 0 0 0-2h-2a1 1 0 0 0 0 2zM11 2v2a1 1 0 0 0 2 0V2a1 1 0 0 0-2 0zm0 18v2a1 1 0 0 0 2 0v-2a1 1 0 0 0-2 0zM5.640 4.220 4.220 5.640l1.420 1.410 1.410-1.410zm12.720 12.730-1.410 1.410 1.410 1.420 1.420-1.420zM19.780 5.640l-1.420-1.420-1.410 1.420 1.410 1.410zM7.050 18.360l-1.410-1.410-1.420 1.410 1.420 1.420z",
        "moon" to "M12 3a9 9 0 1 0 9 9c0-.460-.040-.920-.100-1.360a5.400 5.400 0 0 1-7.540-7.540A9 9 0 0 0 12 3z",
        "key" to "M12.650 10A6 6 0 1 0 12.650 14H17v4h4v-4h2v-4zM7 14a2 2 0 1 1 0-4 2 2 0 0 1 0 4z",
        "clip" to "M16 3h-2.200a2 2 0 0 0-3.600 0H8a2 2 0 0 0-2 2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-1a2 2 0 0 0-2-2zM8 5h8v3H8zM5 7h1v3h12V7h1v12H5z",
        "bolt" to "M11 21h-1l1-7H7.500c-.600 0-.600-.300-.400-.700L13 3h1l-1 7h3.500c.500 0 .600.300.400.700z",
        "text" to "M5 4h14v3h-2V6h-4v12h2v2H9v-2h2V6H7v1H5z",
        // A capital A with the two dots of an umlaut: the letters of other languages.
        "letters" to "M9.2 3a1.3 1.3 0 1 0 0 2.6 1.3 1.3 0 0 0 0-2.6zM14.8 3a1.3 1.3 0 1 0 0 2.6 1.3 1.3 0 0 0 0-2.6zM10.9 8h2.2L19 21h-2.3l-1.4-3.2H8.7L7.3 21H5zM9.6 15.8h4.8L12 10.4z",
        "full" to WINDOW,
        // Places: a window with the part filled where the app's window goes. Halves split it at 12; thirds are drawn as
        // three columns (two divisions), so at 18 dp a third is a different shape from a half, not a slightly narrower one.
        "p3l" to WINDOW + "M8.5 6v12h5V6zM15.5 6v12H20V6z",
        "p3m" to WINDOW + "M4 6v12h4.5V6zM15.5 6v12H20V6z",
        "p3r" to WINDOW + "M4 6v12h4.5V6zM10.5 6v12h5V6z",
        "p23l" to WINDOW + "M15.5 6v12H20V6z",
        "p23r" to WINDOW + "M4 6v12h4.5V6z",
        "ptl" to WINDOW + "M12 6v6H4v6h16V6z",
        "ptr" to WINDOW + "M4 6v12h16v-6h-8V6z",
        "pbl" to WINDOW + "M4 6v6h8v6h8V6z",
        "pbr" to WINDOW + "M4 6v12h8v-6h8V6z",
        "pc" to WINDOW + "M4 6v12h16V6zM8 9h8v6H8z",
        "more" to "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z",
        "pin" to "M16 3v2h-1v6l2 3v2h-4v5l-1 1-1-1v-5H7v-2l2-3V5H8V3z",
        // "Don't suggest": an eye, struck through.
        "hide" to "M12 6a9.770 9.770 0 0 1 8.820 5.500 9.650 9.650 0 0 1-2.410 3.120l1.410 1.410A11.800 11.800 0 0 0 23 11.500C21.270 7.110 17 4 12 4a11.640 11.640 0 0 0-3.640.590L10 6.240A9.800 9.800 0 0 1 12 6zm-1.070 1.140L13 9.210a2.500 2.500 0 0 1 1.280 1.280l2.070 2.070A4.480 4.480 0 0 0 16.500 11.500 4.500 4.500 0 0 0 12 7a4.480 4.480 0 0 0-1.070.140zM2.010 3.870l2.680 2.680A11.740 11.740 0 0 0 1 11.500C2.730 15.890 7 19 12 19a11.730 11.730 0 0 0 4.320-.820l3.420 3.420 1.410-1.410L3.420 2.450zm7.500 7.500 2.610 2.610A2.500 2.500 0 0 1 9.510 11.370zm-3.400-3.400 1.750 1.750A4.490 4.490 0 0 0 12 16a4.480 4.480 0 0 0 1.280-.190l.980.980A9.770 9.770 0 0 1 12 17a9.770 9.770 0 0 1-8.820-5.500 9.900 9.900 0 0 1 2.930-3.530z",
        "list" to "M4 5h3v3H4zm5 0h11v3H9zM4 10.5h3v3H4zm5 0h11v3H9zM4 16h3v3H4zm5 0h11v3H9z",
        // The Booklight window's rows. A disc, half filled: light or dark. A pane with two glints: glass. Two sheets: the
        // shadow one casts on the other. Arrows apart: unfolding. A bulb: a tip.
        "contrast" to "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM12 4v16a8 8 0 0 1 0-16z",
        "glass" to "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zm0 2v14h14V5zM7 12.5 12.5 7H15l-8 8zM9.5 17 17 9.5V12l-5 5z",
        "layers" to "M11.99 18.54l-7.37-5.73L3 14.07l9 7 9-7-1.63-1.27zM12 16l7.36-5.73L21 9l-9-7-9 7 1.63 1.27z",
        "unfold" to "M12 5.83 15.17 9l1.41-1.41L12 3 7.41 7.59 8.83 9zM12 18.17 8.83 15l-1.41 1.41L12 21l4.59-4.59L15.17 15z",
        "bulb" to "M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9zM12 2C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z",
        // A painter's palette: how Booklight looks.
        "palette" to "M12 3a9 9 0 0 0 0 18c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3-4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm5 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3 4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z",
        "user" to "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.3 0-8 1.7-8 5v1h16v-1c0-3.3-4.7-5-8-5z",
        "folder" to "M10 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-8zM4 8h16v10H4z",
        "music" to "M12 3v10.550A4 4 0 1 0 14 17V7h4V3z",
        "file" to "M6 2h8l6 6v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm0 2v16h12V9h-5V4z",
        "sheet" to "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm0 2v4h5V6zm7 0v4h9V6zM4 12v6h5v-6zm7 0v6h9v-6z",
        "slides" to "M3 4h18v2h-1v10h-7v2.5l2 1.5H9l2-1.5V16H4V6H3zm3 2v8h12V6z",
        "bell" to "M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6v-5c0-3.070-1.630-5.640-4.500-6.320V4a1.500 1.500 0 0 0-3 0v.680C7.640 5.360 6 7.920 6 11v5l-2 2v1h16v-1zm-2 1H8v-6c0-2.480 1.510-4.500 4-4.500s4 2.020 4 4.500z",
        "send" to "M3 20.500v-7L14 12 3 10.500v-7L22 12z",
        // A road sign with a turn: directions. A camera: a meeting. A speech bubble with lines: a chat.
        "directions" to "M21.71 11.29l-9-9c-.39-.39-1.02-.39-1.41 0l-9 9c-.39.39-.39 1.02 0 1.41l9 9c.39.39 1.02.39 1.41 0l9-9c.39-.38.39-1.01 0-1.41zM14 14.5V12h-4v3H8v-4c0-.55.45-1 1-1h5V7.5l3.5 3.5-3.5 3.5z",
        "video" to "M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z",
        "chat" to "M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z",
        "battery" to "M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4z",
        "plane" to "M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5z",
        "arrow" to "M20 11H7.830l5.590-5.590L12 4l-8 8 8 8 1.410-1.410L7.830 13H20z",
        "backspace" to "M22 5H9a2 2 0 0 0-1.600.800L2 12l5.400 6.200A2 2 0 0 0 9 19h13a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1zm-3.300 10.300-1.400 1.400L14.500 14l-2.800 2.700-1.400-1.400L13.100 12.500 10.300 9.700l1.400-1.400L14.500 11l2.800-2.700 1.400 1.400-2.800 2.800z",
    ).mapValues { (k, v) -> icon(k, v) }

    fun of(name: String): ImageVector = when (name) {
        "search" -> search
        "settings" -> settings
        "globe" -> globe
        "calc" -> calc
        "booklight" -> booklight
        "enter" -> enter
        "check" -> check
        else -> more[name] ?: app
    }
}

/** App icons as bitmaps, loaded off the main thread and kept for the life of the process. */
class AppIcons(context: Context) {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val users = context.getSystemService(UserManager::class.java)
    private val density = context.resources.displayMetrics.densityDpi
    private val cache = LruCache<Icon.App, ImageBitmap>(300)

    fun cached(icon: Icon.App): ImageBitmap? = cache.get(icon)

    suspend fun load(icon: Icon.App, sizePx: Int): ImageBitmap? = cache.get(icon) ?: withContext(Dispatchers.IO) {
        runCatching {
            val user = users.getUserForSerialNumber(icon.user) ?: android.os.Process.myUserHandle()
            val info = launcher.getActivityList(icon.packageName, user).firstOrNull { it.componentName.className == icon.className }
            info?.getIcon(density)?.toBitmap(sizePx, sizePx)?.asImageBitmap()?.also { cache.put(icon, it) }
        }.getOrNull()
    }
}
