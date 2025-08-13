package thesis.project.omrscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Blue200,
    primaryContainer = Blue700,
    secondary = Teal200,
    secondaryContainer = Teal700,
    background = White,
    surface = White,
    onPrimary = White,
    onSecondary = Black,
    onBackground = Black,
    onSurface = Black,
)

private val DarkColors = darkColorScheme(
    primary = Blue200,
    primaryContainer = Blue700,
    secondary = Teal200,
    secondaryContainer = Teal700,
    background = Black,
    surface = Black,
    onPrimary = Black,
    onSecondary = Black,
    onBackground = White,
    onSurface = White,
)

@Composable
fun ThemeAww(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
