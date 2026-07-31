package matchsong.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * M2.2 Design System 的入口占位；颜色/排版/形状令牌在 M2.2 细化。
 */
private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun MatchSongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
