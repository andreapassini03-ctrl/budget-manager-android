// File: ui/theme/Theme.kt (per il tema scuro)
package com.budgetapp.budgetapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Palette base (fallback) pensata per un look caldo / professionale
// Orange come primary, teal come secondary complementare, un tocco di violet per accent/tertiary
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF9100),        // Orange 600
    onPrimary = Color(0xFF171717),
    primaryContainer = Color(0xFFFFB347),
    onPrimaryContainer = Color(0xFF2C1600),

    secondary = Color(0xFF26A69A),      // Teal 400
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D43),
    onSecondaryContainer = Color(0xFFB2FFF2),

    tertiary = Color(0xFFB388FF),       // Lavender accent
    onTertiary = Color(0xFF280047),
    tertiaryContainer = Color(0xFF43215F),
    onTertiaryContainer = Color(0xFFEEDCFF),

    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB4B4B4),
    outline = Color(0xFF535353),

    error = Color(0xFFFF5370),
    onError = Color.Black,
    errorContainer = Color(0xFF8C1D27),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9800),        // Orange 500
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9B0),
    onPrimaryContainer = Color(0xFF301800),

    secondary = Color(0xFF00897B),      // Teal 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF6FFADF),
    onSecondaryContainer = Color(0xFF00201B),

    tertiary = Color(0xFF7E57C2),       // Deep purple accent
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9DDFF),
    onTertiaryContainer = Color(0xFF300E63),

    background = Color(0xFFFDFCF9),
    onBackground = Color(0xFF1E1E1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202020),
    surfaceVariant = Color(0xFFF1E4D9),
    onSurfaceVariant = Color(0xFF52443A),
    outline = Color(0xFF857468),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002)
)

/**
 * Tema applicativo centralizzato.
 * dynamicColor: se true (e Android 12+), usa i colori dinamici Material You del device.
 */
@Composable
fun MyWalletTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Mantieni la tua Typography esistente
        content = content
    )
}