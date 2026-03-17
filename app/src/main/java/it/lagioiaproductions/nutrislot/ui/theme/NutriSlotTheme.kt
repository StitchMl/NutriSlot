package it.lagioiaproductions.nutrislot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NutriSlotLightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF5F6368),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EAED),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF0F9D58),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFF4DE),
    onTertiaryContainer = Color(0xFF002111),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFEFF3F9),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF747775)
)

private val NutriSlotDarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = Color(0xFFC4C7C5),
    onSecondary = Color(0xFF2B3133),
    secondaryContainer = Color(0xFF3A4043),
    onSecondaryContainer = Color(0xFFE8EAED),
    tertiary = Color(0xFF7EE2A8),
    onTertiary = Color(0xFF00391D),
    tertiaryContainer = Color(0xFF00522C),
    onTertiaryContainer = Color(0xFFCFF4DE),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF181C22),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF2A2F36),
    onSurfaceVariant = Color(0xFFC4C7D0),
    outline = Color(0xFF8E9099)
)

private val NutriSlotTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
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
        lineHeight = 20.sp
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
fun NutriSlotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> NutriSlotDarkColors
        else -> NutriSlotLightColors
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