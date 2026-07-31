package matchsong.app.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * M2.2-1 Design System 令牌层：色板。
 *
 * 新增颜色先加这里，业务页禁止硬编码 Color 值（FR-SHELL-2）。
 * MVP 以浅色为主；深色为保守映射（M2 不深入适配，SPEC 未要求）。
 */
object MatchSongColors {
    val Primary = Color(0xFF1DB954) // 品牌绿（与启动图标一致）
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFB7F5C9)
    val OnPrimaryContainer = Color(0xFF00210C)
    val Secondary = Color(0xFF506357)
    val Error = Color(0xFFBA1A1A)
    val Warning = Color(0xFFF4B400) // 质量警告色（削波/噪声提示）
    val Success = Color(0xFF1DB954)
    val Background = Color(0xFFFBFDF9)
    val Surface = Color(0xFFFFFFFF)
    val Outline = Color(0xFF70796F)
}

private val LightColors =
    lightColorScheme(
        primary = MatchSongColors.Primary,
        onPrimary = MatchSongColors.OnPrimary,
        primaryContainer = MatchSongColors.PrimaryContainer,
        onPrimaryContainer = MatchSongColors.OnPrimaryContainer,
        secondary = MatchSongColors.Secondary,
        error = MatchSongColors.Error,
        background = MatchSongColors.Background,
        surface = MatchSongColors.Surface,
        outline = MatchSongColors.Outline,
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8CD69F),
        onPrimary = Color(0xFF00391C),
        error = Color(0xFFFFB4AB),
        background = Color(0xFF101413),
        surface = Color(0xFF101413),
    )

@Composable
fun MatchSongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MatchSongType.Typography,
        shapes = MatchSongShape.Shapes,
        content = content,
    )
}
