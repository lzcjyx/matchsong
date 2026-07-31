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
 * M2.2-3 通用状态组件：Permission（说明 + 重试/去设置）。
 *
 * M3 权限状态机接入后由 Recording 页使用；M2 阶段不请求真实权限。
 */
@Composable
fun PermissionState(
    title: String = "需要麦克风权限",
    description: String = "我们需要麦克风权限来录制你的演唱并分析音域。音频仅在本机处理，不会上传。",
    onRetry: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = MatchSongSpacing.S),
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = MatchSongSpacing.M)) { Text("重试") }
        if (onOpenSettings != null) {
            TextButton(onClick = onOpenSettings) { Text("去系统设置开启") }
        }
    }
}
