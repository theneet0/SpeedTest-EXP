package com.example.speedlab.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.speedlab.model.ThemeMode

val SpeedLabNavy = Color(0xFF071B33)
val SpeedLabAqua = Color(0xFF28D7C0)
val SpeedLabBlue = Color(0xFF4B8FFF)
val SpeedLabMist = Color(0xFFF4F7FB)
val SpeedLabMuted = Color(0xFF64748B)

private val LightColors = lightColorScheme(
    primary = SpeedLabNavy,
    onPrimary = Color.White,
    secondary = Color(0xFF007F73),
    onSecondary = Color.White,
    tertiary = SpeedLabBlue,
    background = SpeedLabMist,
    onBackground = SpeedLabNavy,
    surface = Color.White,
    onSurface = SpeedLabNavy,
    surfaceVariant = Color(0xFFE8EFF5),
    onSurfaceVariant = Color(0xFF526174),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = SpeedLabAqua,
    onPrimary = Color(0xFF003731),
    secondary = Color(0xFF70E7D7),
    onSecondary = Color(0xFF003731),
    tertiary = Color(0xFF9EC0FF),
    background = Color(0xFF07111F),
    onBackground = Color(0xFFE6EDF6),
    surface = Color(0xFF0E2034),
    onSurface = Color(0xFFE6EDF6),
    surfaceVariant = Color(0xFF1C3146),
    onSurfaceVariant = Color(0xFFB9C8D8),
    error = Color(0xFFFFB4AB),
)

private val SpeedLabTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    ),
)

@Composable
fun SpeedLabTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = SpeedLabTypography,
        content = content,
    )
}
