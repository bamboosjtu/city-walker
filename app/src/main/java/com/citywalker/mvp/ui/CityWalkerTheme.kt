package com.citywalker.mvp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF0B1F22)
val DeepTeal = Color(0xFF12383A)
val Seafoam = Color(0xFFA7D0C0)
val Paper = Color(0xFFF4F0E8)
val Sand = Color(0xFFE5D8C3)
val Orange = Color(0xFFE9A15B)
val Coral = Color(0xFFE26F5B)
val Slate = Color(0xFF91A6A2)
val MutedInk = Color(0xFF566A67)

private val CityWalkerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = 26.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    )
}

@Composable
fun CityWalkerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Orange,
            onPrimary = Ink,
            primaryContainer = Orange.copy(alpha = .18f),
            onPrimaryContainer = Paper,
            secondary = Seafoam,
            onSecondary = Ink,
            secondaryContainer = Seafoam.copy(alpha = .16f),
            onSecondaryContainer = Paper,
            background = Ink,
            onBackground = Paper,
            surface = DeepTeal,
            onSurface = Paper,
            surfaceVariant = Color(0xFF244C4C),
            onSurfaceVariant = Sand,
            outline = Color(0xFF53706B),
            error = Coral,
            onError = Paper
        ),
        typography = CityWalkerTypography,
        content = content
    )
}
