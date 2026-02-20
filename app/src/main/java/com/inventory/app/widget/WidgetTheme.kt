package com.inventory.app.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/** Orange expiring-count color — readable in both light and dark. */
val WidgetExpiringColor = ColorProvider(Color(0xFFFF9800))

val WidgetColorProviders = ColorProviders(
    light = lightColorScheme(
        primary = Color(0xFF2E7D32),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA5D6A7),
        onPrimaryContainer = Color(0xFF002204),
        secondary = Color(0xFF526350),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD4E8D0),
        onSecondaryContainer = Color(0xFF101F10),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF8FAF0),
        onBackground = Color(0xFF1A1C18),
        surface = Color(0xFFF8FAF0),
        onSurface = Color(0xFF1A1C18),
        surfaceVariant = Color(0xFFDEE5D9),
        onSurfaceVariant = Color(0xFF424940),
        outline = Color(0xFF72796F)
    ),
    dark = darkColorScheme(
        primary = Color(0xFF81C784),
        onPrimary = Color(0xFF00390B),
        primaryContainer = Color(0xFF005315),
        onPrimaryContainer = Color(0xFFA5D6A7),
        secondary = Color(0xFFB9CCB5),
        onSecondary = Color(0xFF243424),
        secondaryContainer = Color(0xFF3A4B39),
        onSecondaryContainer = Color(0xFFD4E8D0),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE2E3DC),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFE2E3DC),
        surfaceVariant = Color(0xFF1E2A1E),
        onSurfaceVariant = Color(0xFFC2C9BD),
        outline = Color(0xFF8C9389)
    )
)
