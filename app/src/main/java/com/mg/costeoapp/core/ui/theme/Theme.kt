package com.mg.costeoapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CosteoGreenPrimary,
    onPrimary = CosteoOnPrimary,
    primaryContainer = CosteoGreenPrimaryContainer,
    onPrimaryContainer = CosteoOnPrimaryContainer,
    secondary = CosteoOrangeSecondary,
    onSecondary = CosteoOnSecondary,
    secondaryContainer = CosteoOrangeSecondaryContainer,
    onSecondaryContainer = CosteoOnSecondaryContainer,
    tertiary = CosteoTertiary,
    onTertiary = CosteoOnTertiary,
    tertiaryContainer = CosteoTertiaryContainer,
    onTertiaryContainer = CosteoOnTertiaryContainer,
    error = CosteoError,
    onError = CosteoOnError,
    errorContainer = CosteoErrorContainer,
    onErrorContainer = CosteoOnErrorContainer,
    surface = CosteoSurface,
    onSurface = CosteoOnSurface,
    surfaceVariant = CosteoSurfaceVariant,
    onSurfaceVariant = CosteoOnSurfaceVariant,
    outline = CosteoOutline,
    outlineVariant = CosteoOutlineVariant,
    background = CosteoBackground,
    onBackground = CosteoOnBackground,
    inverseSurface = CosteoInverseSurface,
    inverseOnSurface = CosteoInverseOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = CosteoDarkPrimary,
    onPrimary = CosteoDarkOnPrimary,
    primaryContainer = CosteoDarkPrimaryContainer,
    onPrimaryContainer = CosteoDarkOnPrimaryContainer,
    secondary = CosteoDarkSecondary,
    onSecondary = CosteoDarkOnSecondary,
    secondaryContainer = CosteoDarkSecondaryContainer,
    onSecondaryContainer = CosteoDarkOnSecondaryContainer,
    tertiary = CosteoTertiary,
    onTertiary = CosteoOnTertiary,
    tertiaryContainer = CosteoTertiaryContainer,
    onTertiaryContainer = CosteoOnTertiaryContainer,
    error = CosteoError,
    onError = CosteoOnError,
    errorContainer = CosteoErrorContainer,
    onErrorContainer = CosteoOnErrorContainer,
    surface = CosteoDarkSurface,
    onSurface = CosteoDarkOnSurface,
    surfaceVariant = CosteoDarkSurfaceVariant,
    onSurfaceVariant = CosteoDarkOnSurfaceVariant,
    outline = CosteoDarkOutline,
    outlineVariant = CosteoDarkOutlineVariant,
    background = CosteoDarkBackground,
    onBackground = CosteoDarkOnBackground,
    inverseSurface = CosteoDarkInverseSurface,
    inverseOnSurface = CosteoDarkInverseOnSurface
)

@Composable
fun CosteoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CosteoTypography,
        content = content
    )
}
