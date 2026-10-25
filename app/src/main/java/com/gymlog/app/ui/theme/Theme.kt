package com.gymlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Custom colors
data class GymColors(
    val bluePrimary: Color,
    val blueOnPrimary: Color,
    val bluePrimaryContainer: Color,
    val blueOnPrimaryContainer: Color,
    val blueSecondaryContainer: Color,
    val blueOnSecondaryContainer: Color,
    val blueSurface: Color,
    val blueSurfaceVariant: Color,
    val blueOnSurface: Color,
    val blueOnSurfaceVariant: Color,
    val blueBackground: Color
)

val LocalGymColors = staticCompositionLocalOf {
    GymColors(
        bluePrimary = BluePrimary,
        blueOnPrimary = BlueOnPrimary,
        bluePrimaryContainer = BluePrimaryContainer,
        blueOnPrimaryContainer = BlueOnPrimaryContainer,
        blueSecondaryContainer = BlueSecondaryContainer,
        blueOnSecondaryContainer = BlueOnSecondaryContainer,
        blueSurface = BlueSurface,
        blueSurfaceVariant = BlueSurfaceVariant,
        blueOnSurface = BlueOnSurface,
        blueOnSurfaceVariant = BlueOnSurfaceVariant,
        blueBackground = BlueBackground
    )
}

// Workout colors
data class WorkoutColors(
    val warmup: Color,
    val warmupBackground: Color,
    val warmupCompletedBackground: Color,
    val completedSet: Color,
    val up: Color,
    val upBackground: Color,
    val down: Color,
    val downBackground: Color
)

val LocalWorkoutColors = staticCompositionLocalOf {
    WorkoutColors(
        warmup = WarmupColor,
        warmupBackground = WarmupBackground,
        warmupCompletedBackground = WarmupCompletedBackground,
        completedSet = CompletedSetColor,
        up = UpColor,
        upBackground = UpBackground,
        down = DownColor,
        downBackground = DownBackground
    )
}

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    surface = BlueSurface,
    surfaceVariant = BlueSurfaceVariant,
    onSurface = BlueOnSurface,
    onSurfaceVariant = BlueOnSurfaceVariant,
    background = BlueBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    surface = BlueSurface,
    surfaceVariant = BlueSurfaceVariant,
    onSurface = BlueOnSurface,
    onSurfaceVariant = BlueOnSurfaceVariant,
    background = BlueBackground
)

@Composable
fun GymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalGymColors provides LocalGymColors.current,
        LocalWorkoutColors provides LocalWorkoutColors.current
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}