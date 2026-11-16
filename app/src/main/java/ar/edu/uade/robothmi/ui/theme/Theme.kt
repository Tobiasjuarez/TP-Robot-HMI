package ar.edu.uade.robothmi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Placeholder: reemplazar por los colores/tipografía del Figma.
private val LightColors = lightColorScheme(primary = Color(0xFF1A5FB4))
private val DarkColors = darkColorScheme(primary = Color(0xFF8AB4F8))

@Composable
fun RobotHmiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
