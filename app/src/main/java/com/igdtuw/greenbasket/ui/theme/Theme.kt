package com.igdtuw.greenbasket.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
// --- Producer Color Scheme (Green & White) ---
private val ProducerLightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = White,
    primaryContainer = Green200,
    onPrimaryContainer = TextColorDark,
    secondary = Green500,
    onSecondary = White,
    background = OffWhite,
    onBackground = TextColorDark,
    surface = White,
    onSurface = TextColorDark,
    error = Color(0xFFB00020),
    onError = White
)

private val ProducerDarkColorScheme = darkColorScheme(
    primary = Green200,
    onPrimary = TextColorDark,
    primaryContainer = Green500,
    onPrimaryContainer = White,
    secondary = Green700,
    onSecondary = White,
    background = Color(0xFF121212),
    onBackground = OffWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = OffWhite,
    error = Color(0xFFCF6679),
    onError = Color.Black
)


// --- Consumer Color Scheme (Multi-colored & White) ---
private val ConsumerLightColorScheme = lightColorScheme(
    primary = ConsumerPrimary,
    onPrimary = White,
    primaryContainer = ConsumerSecondary,
    onPrimaryContainer = TextColorDark,
    secondary = ConsumerSecondary,
    onSecondary = TextColorDark,
    background = ConsumerBackground,
    onBackground = TextColorDark,
    surface = White,
    onSurface = TextColorDark,
    error = Color(0xFFB00020),
    onError = White
)

private val ConsumerDarkColorScheme = darkColorScheme(
    primary = ConsumerPrimaryVariant,
    onPrimary = White,
    primaryContainer = ConsumerSecondary,
    onPrimaryContainer = TextColorDark,
    secondary = ConsumerSecondary,
    onSecondary = TextColorDark,
    background = Color(0xFF121212),
    onBackground = OffWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = OffWhite,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenBasketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // default to system theme
    isProducer: Boolean, // must be explicitly passed by caller
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isProducer -> if (darkTheme) ProducerDarkColorScheme else ProducerLightColorScheme
        else -> if (darkTheme) ConsumerDarkColorScheme else ConsumerLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()

            // Adjust for light/dark status bar icon visibility
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme // true for dark text/icons on light background
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
