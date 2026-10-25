package com.gymlog.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun GymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalWorkoutColors provides LocalWorkoutColors.current) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}