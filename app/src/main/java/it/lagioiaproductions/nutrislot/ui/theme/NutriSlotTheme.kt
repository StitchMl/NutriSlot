package it.lagioiaproductions.nutrislot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NutriSlotLightColors = lightColorScheme(
    primary = NutriAccent,
    onPrimary = NutriWarmSurface,
    primaryContainer = NutriAccentSoft,
    onPrimaryContainer = NutriTextPrimary,
    secondary = NutriOlive,
    onSecondary = NutriWarmSurface,
    secondaryContainer = NutriCard,
    onSecondaryContainer = NutriTextPrimary,
    tertiary = NutriOlive,
    onTertiary = NutriWarmSurface,
    tertiaryContainer = NutriCard,
    onTertiaryContainer = NutriTextPrimary,
    error = NutriError,
    onError = NutriWarmSurface,
    errorContainer = NutriAccentSoft,
    onErrorContainer = NutriTextPrimary,
    background = NutriCream,
    onBackground = NutriTextPrimary,
    surface = NutriWarmSurface,
    onSurface = NutriTextPrimary,
    surfaceVariant = NutriCard,
    onSurfaceVariant = NutriTextSecondary,
    outline = NutriOutline
)

private val NutriSlotDarkColors = darkColorScheme(
    primary = NutriDarkAccent,
    onPrimary = NutriDarkBackground,
    primaryContainer = NutriDarkCard,
    onPrimaryContainer = NutriDarkText,
    secondary = NutriDarkAccent,
    onSecondary = NutriDarkBackground,
    secondaryContainer = NutriDarkCard,
    onSecondaryContainer = NutriDarkText,
    tertiary = NutriDarkAccent,
    onTertiary = NutriDarkBackground,
    tertiaryContainer = NutriDarkCard,
    onTertiaryContainer = NutriDarkText,
    error = NutriError,
    onError = NutriWarmSurface,
    errorContainer = NutriDarkCard,
    onErrorContainer = NutriDarkText,
    background = NutriDarkBackground,
    onBackground = NutriDarkText,
    surface = NutriDarkSurface,
    onSurface = NutriDarkText,
    surfaceVariant = NutriDarkCard,
    onSurfaceVariant = NutriDarkSubtleText,
    outline = NutriDarkOutline
)

private val NutriSlotTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

private val NutriSlotShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
@Suppress("unused")
fun NutriSlotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        NutriSlotDarkColors
    } else {
        NutriSlotLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NutriSlotTypography,
        shapes = NutriSlotShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}