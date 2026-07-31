package matchsong.app.design.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import matchsong.app.design.MatchSongSpacing

/**
 * M2.2-3 通用状态组件：Empty（可选动作）。
 */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.padding(top = MatchSongSpacing.S)) {
                Text(actionText)
            }
        }
    }
}
