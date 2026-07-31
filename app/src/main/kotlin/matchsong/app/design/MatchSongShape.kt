package matchsong.app.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * M2.2-1 Design System 令牌层：Shape（圆角等级）。
 *
 * 业务页使用 MaterialTheme.shapes，禁止硬编码圆角（FR-SHELL-2）。
 */
object MatchSongShape {
    val Shapes =
        Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(24.dp),
        )
}
