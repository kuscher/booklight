package io.github.kuscher.booklight.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import java.io.File

/**
 * Google Sans Flex straight from the device (Googlebooks ship it in /product/fonts), with the
 * rounded cut Material 3 Expressive uses. Elsewhere, the system's default font.
 */
object Fonts {
    private val file = File("/product/fonts/GoogleSansFlex-Regular.ttf")

    @OptIn(ExperimentalTextApi::class)
    private fun family(round: Float): FontFamily =
        if (!file.canRead()) FontFamily.Default
        else FontFamily(listOf(400, 500, 600, 700).map { w ->
            Font(file, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("ROND", round)))
        })

    val text: FontFamily by lazy { family(0f) }
    val round: FontFamily by lazy { family(100f) }
}

/** Booklight's own colours, for people who don't want the wallpaper's: a quiet blue on neutral greys. */
private val OwnLight = lightColorScheme(
    primary = Color(0xFF3A5BA9), onPrimary = Color.White, primaryContainer = Color(0xFFD9E2FF), onPrimaryContainer = Color(0xFF0F2A5C),
    secondary = Color(0xFF575E71), secondaryContainer = Color(0xFFC3D2FA), onSecondaryContainer = Color(0xFF0F2A5C),
    tertiary = Color(0xFF8B4A8F), tertiaryContainer = Color(0xFFFFD6FA), onTertiaryContainer = Color(0xFF36003E),
    surface = Color(0xFFF9F9FF), surfaceBright = Color(0xFFF9F9FF), surfaceDim = Color(0xFFD9D9E0), onSurface = Color(0xFF171C2B), onSurfaceVariant = Color(0xFF44474F), outline = Color(0xFF757780), outlineVariant = Color(0xFFC5C6D0),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F3FA), surfaceContainer = Color(0xFFEDEDF4), surfaceContainerHigh = Color(0xFFE7E8EE), surfaceContainerHighest = Color(0xFFE2E2E9),
    background = Color(0xFFF9F9FF), onBackground = Color(0xFF171C2B),
)
private val OwnDark = darkColorScheme(
    primary = Color(0xFFAFC6FF), onPrimary = Color(0xFF002D6E), primaryContainer = Color(0xFF1F4390), onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFBFC6DC), secondaryContainer = Color(0xFF33456F), onSecondaryContainer = Color(0xFFD9E3FF),
    tertiary = Color(0xFFE3B7E6), tertiaryContainer = Color(0xFF70327A), onTertiaryContainer = Color(0xFFFFD6FA),
    surface = Color(0xFF11131A), surfaceBright = Color(0xFF2F313A), surfaceDim = Color(0xFF11131A), onSurface = Color(0xFFE3E6F2), onSurfaceVariant = Color(0xFFC5C6D0), outline = Color(0xFF8F909A), outlineVariant = Color(0xFF44474F),
    surfaceContainerLowest = Color(0xFF0E1016), surfaceContainerLow = Color(0xFF191B23), surfaceContainer = Color(0xFF1D1F27), surfaceContainerHigh = Color(0xFF282A32), surfaceContainerHighest = Color(0xFF33353D),
    background = Color(0xFF11131A), onBackground = Color(0xFFE3E6F2),
)

/**
 * The colours of a flight's badge: green for on time and early, amber for late. The one place in the
 * panel besides the selection that has a colour, and the one exception to "a delay is said, not
 * painted" (Alex, 2 October 2026; docs/design/flights-row). Fixed, in both schemes: they are not the
 * wallpaper's, and mean the same on every device. Never red: that stays the colour of what removes
 * something.
 *
 * On glass a colour is always a fill with its own ink on it ([good] under [onGood], [late] under
 * [onLate]): a green or amber word straight on the glass does not hold over the wrong window. On the
 * pinned window's solid ground the word itself carries the colour ([goodWord], [lateWord]).
 */
class FlightColors(val good: Color, val onGood: Color, val late: Color, val onLate: Color, val goodWord: Color, val lateWord: Color) {
    companion object {
        private val light = FlightColors(good = Color(0xFFB6F2BE), onGood = Color(0xFF00210A), late = Color(0xFFFFDEA6), onLate = Color(0xFF261900), goodWord = Color(0xFF17662B), lateWord = Color(0xFF7A5200))
        private val dark = FlightColors(good = Color(0xFF0A5226), onGood = Color(0xFFB6F2BE), late = Color(0xFF5C4300), onLate = Color(0xFFFFDEA6), goodWord = Color(0xFF8FDB9B), lateWord = Color(0xFFF1C267))
        fun of(dark: Boolean): FlightColors = if (dark) this.dark else light
    }
}

/** Whether Booklight is dark: the user's choice (`light`, `dark`), else the system's. */
@Composable
fun isDark(theme: String): Boolean = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }

/**
 * The light scheme, whatever the theme is: first run's welcome is one night in both themes, and the one coloured
 * surface in it, the knife's handle, is the selection as light theme has it. The device's own colours when [tint] is
 * on, else Booklight's own.
 */
@Composable
fun lightScheme(tint: Boolean): ColorScheme = if (tint) dynamicLightColorScheme(LocalContext.current) else OwnLight

/** The device's own colours (Material You, from the wallpaper) when [tint] is on, else Booklight's own; light or dark. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BooklightTheme(dark: Boolean = isSystemInDarkTheme(), tint: Boolean = true, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = when {
        tint -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> if (dark) OwnDark else OwnLight
    }
    val base = Typography()
    val type = Typography(
        displaySmall = base.displaySmall.copy(fontFamily = Fonts.round),
        // A page's title in the Booklight window: 28/36 at 600 in the rounded cut.
        headlineMedium = base.headlineMedium.copy(fontFamily = Fonts.round, fontWeight = FontWeight(600)),
        headlineSmall = base.headlineSmall.copy(fontFamily = Fonts.round),
        titleLarge = base.titleLarge.copy(fontFamily = Fonts.text),
        titleMedium = base.titleMedium.copy(fontFamily = Fonts.text),
        titleSmall = base.titleSmall.copy(fontFamily = Fonts.text),
        bodyLarge = base.bodyLarge.copy(fontFamily = Fonts.text),
        bodyMedium = base.bodyMedium.copy(fontFamily = Fonts.text),
        bodySmall = base.bodySmall.copy(fontFamily = Fonts.text),
        labelLarge = base.labelLarge.copy(fontFamily = Fonts.text),
        labelMedium = base.labelMedium.copy(fontFamily = Fonts.text),
        labelSmall = base.labelSmall.copy(fontFamily = Fonts.text),
    )
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), typography = type, content = content)
}
