package io.github.kuscher.booklight.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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

/** The device's own colours (Material You, from the wallpaper), light or dark with the system. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BooklightTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val base = Typography()
    val type = Typography(
        displaySmall = base.displaySmall.copy(fontFamily = Fonts.round),
        headlineSmall = base.headlineSmall.copy(fontFamily = Fonts.round),
        titleLarge = base.titleLarge.copy(fontFamily = Fonts.text),
        titleMedium = base.titleMedium.copy(fontFamily = Fonts.text),
        bodyLarge = base.bodyLarge.copy(fontFamily = Fonts.text),
        bodyMedium = base.bodyMedium.copy(fontFamily = Fonts.text),
        bodySmall = base.bodySmall.copy(fontFamily = Fonts.text),
        labelLarge = base.labelLarge.copy(fontFamily = Fonts.text),
        labelMedium = base.labelMedium.copy(fontFamily = Fonts.text),
        labelSmall = base.labelSmall.copy(fontFamily = Fonts.text),
    )
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), typography = type, content = content)
}
