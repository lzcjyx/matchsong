package matchsong.app.design.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import matchsong.app.design.MatchSongSpacing

/**
 * M2.2-2 页面顶部栏：统一标题与返回动作。
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MatchSongSpacing.M, vertical = MatchSongSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("返回") }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = MatchSongSpacing.S),
        )
        actions?.invoke()
    }
}
