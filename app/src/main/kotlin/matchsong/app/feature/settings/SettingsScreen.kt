package matchsong.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** M8 设置页（M2 占位：删除数据入口 + 确认弹窗）。 */
@Composable
fun SettingsScreen(onDeleteAll: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "设置", style = MaterialTheme.typography.headlineMedium)
        Text(text = "（M2 占位：设置项在 M8/M9 实现）", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { showConfirm = true }, modifier = Modifier.padding(top = 24.dp)) { Text("删除全部数据") }
    }

    if (showConfirm) {
        DeleteConfirmDialog(
            onConfirm = {
                showConfirm = false
                onDeleteAll()
            },
            onCancel = { showConfirm = false },
        )
    }
}

/** M2.5-2 数据删除确认弹窗（真实删除逻辑在 M9）。 */
@Composable
fun DeleteConfirmScreen(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    DeleteConfirmDialog(onConfirm = onConfirm, onCancel = onCancel)
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("删除全部数据？") },
        text = { Text("将删除历史记录、收藏、缓存音频与设置。此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        },
    )
}
