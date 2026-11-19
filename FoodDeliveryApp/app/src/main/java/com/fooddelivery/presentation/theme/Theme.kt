package com.fooddelivery.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = ForestSoft,
    onPrimaryContainer = ForestDeep,
    inversePrimary = Lime,
    secondary = Fresh,
    onSecondary = Color.White,
    secondaryContainer = FreshSoft,
    onSecondaryContainer = Color(0xFF34245F),
    tertiary = Butter,
    onTertiary = ButterDeep,
    tertiaryContainer = ButterSoft,
    onTertiaryContainer = ButterDeep,
    background = Mint,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = MintVariant,
    onSurfaceVariant = InkSoft,
    surfaceTint = Forest,
    inverseSurface = Ink,
    inverseOnSurface = Mint,
    outline = Color(0xFFC8BEDA),
    outlineVariant = MintLine,
    error = Berry,
    onError = Color.White,
    errorContainer = BerrySoft,
    onErrorContainer = Color(0xFF68152A),
    scrim = Color(0xFF1C132B),
)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF271747),
    primaryContainer = Color(0xFF4B3478),
    onPrimaryContainer = ForestSoft,
    inversePrimary = Forest,
    secondary = Color(0xFFC4B7FF),
    onSecondary = Color(0xFF2C2050),
    secondaryContainer = Color(0xFF40345F),
    onSecondaryContainer = FreshSoft,
    tertiary = Butter,
    onTertiary = ButterDeep,
    tertiaryContainer = Color(0xFF493B66),
    onTertiaryContainer = ButterSoft,
    background = Pine,
    onBackground = PineText,
    surface = PineCard,
    onSurface = PineText,
    surfaceVariant = PineVariant,
    onSurfaceVariant = PineTextSoft,
    surfaceTint = Lime,
    inverseSurface = PineText,
    inverseOnSurface = Pine,
    outline = Color(0xFF82749C),
    outlineVariant = Color(0xFF4A3B60),
    error = Color(0xFFFF9BAE),
    onError = Color(0xFF4B091C),
    errorContainer = Color(0xFF64253A),
    onErrorContainer = BerrySoft,
    scrim = Color.Black,
)

@Composable
fun FoodDeliveryTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
