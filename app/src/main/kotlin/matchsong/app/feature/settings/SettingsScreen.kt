package matchsong.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** M9.3 粒度删除目标（确认弹窗文案 + ViewModel 动作）。 */
private enum class DeleteTarget(
    val title: String,
    val message: String,
) {
    HISTORY("删除全部历史记录？", "将删除全部分析历史。此操作不可撤销。"),
    FAVORITES("清空收藏？", "将取消收藏全部歌曲。此操作不可撤销。"),
    SETTINGS("删除设置？", "将恢复默认语言与偏好设置。"),
    CACHE("删除缓存音频？", "将删除临时录音文件。"),
}

/**
 * M9.3 设置页（FR-HX-4：单条历史删除在历史页；本页提供全部历史/收藏/设置/缓存/重置；
 * BUG-018 歌曲包区块）。
 */
@Suppress("LongMethod") // 设置页为声明式区块清单（数据管理 + 歌曲包 + 状态反馈），拆散破坏可读性
@Composable
fun SettingsScreen(
    onDeleteAll: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var confirmTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "设置", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "数据管理", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        ActionRow("删除全部历史记录") { confirmTarget = DeleteTarget.HISTORY }
        ActionRow("清空收藏") { confirmTarget = DeleteTarget.FAVORITES }
        ActionRow("删除设置（偏好）") { confirmTarget = DeleteTarget.SETTINGS }
        ActionRow("删除缓存音频") { confirmTarget = DeleteTarget.CACHE }
        ActionRow("删除全部数据") { onDeleteAll() }

        Spacer(modifier = Modifier.height(16.dp))
        SongPackSection(viewModel)

        Spacer(modifier = Modifier.height(16.dp))
        when (val s = state) {
            is SettingsViewModel.UiState.Busy ->
                if (s.action == SettingsViewModel.Action.IMPORT_PACK) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(text = "正在下载歌曲包…", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(text = "删除中…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            is SettingsViewModel.UiState.Done ->
                if (s.action != SettingsViewModel.Action.IMPORT_PACK) {
                    Text(
                        text = "已删除",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            is SettingsViewModel.UiState.Error ->
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            SettingsViewModel.UiState.Idle -> Unit
            SettingsViewModel.UiState.ResetCompleted -> Unit
        }
    }

    confirmTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmTarget = null },
            title = { Text(target.title) },
            text = { Text(target.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmTarget = null
                        when (target) {
                            DeleteTarget.HISTORY -> viewModel.clearHistory()
                            DeleteTarget.FAVORITES -> viewModel.clearFavorites()
                            DeleteTarget.SETTINGS -> viewModel.clearSettings()
                            DeleteTarget.CACHE -> viewModel.clearCache()
                        }
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(text = label)
    }
}

/** BUG-018：歌曲包区块（联网曲库；下载替换当前曲库）。 */
@Composable
private fun SongPackSection(viewModel: SettingsViewModel) {
    Text(text = "歌曲包（联网曲库）", style = MaterialTheme.typography.titleMedium)
    val songCount by viewModel.songCount.collectAsState()
    Text(
        text = "当前曲库 $songCount 首；下载歌曲包将替换当前曲库（专攻方向）。",
        style = MaterialTheme.typography.bodySmall,
    )
    var packUrl by remember { mutableStateOf(DEFAULT_PACK_URL) }
    OutlinedTextField(
        value = packUrl,
        onValueChange = { packUrl = it },
        label = { Text("歌曲包 URL（HTTPS JSON）") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
    OutlinedButton(
        onClick = { viewModel.importSongPack(packUrl) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text("下载并导入歌曲包")
    }
    val importMessage by viewModel.importMessage.collectAsState()
    if (importMessage != null) {
        Text(
            text = importMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** BUG-018：默认歌曲包（公开仓库 raw；可自行替换为任意 HTTPS JSON）。 */
private const val DEFAULT_PACK_URL =
    "https://raw.githubusercontent.com/lzcjyx/matchsong/main/song-packs/zhou-jaylen-pack.json"

/**
 * M9.3 重置应用确认页（ACC-15）：确认后执行删除全部数据，
 * 成功后经 [onResetCompleted] 由导航层跳回 Splash（重新 Onboarding）。
 */
@Composable
fun DeleteConfirmScreen(
    onResetCompleted: () -> Unit,
    onCancel: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(true) }

    LaunchedEffect(state) {
        if (state is SettingsViewModel.UiState.ResetCompleted) {
            onResetCompleted()
        }
    }

    if (showDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDialog = false
                viewModel.resetAll()
            },
            onCancel = onCancel,
        )
    }

    when (val s = state) {
        is SettingsViewModel.UiState.Busy ->
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(text = "正在删除全部数据…", style = MaterialTheme.typography.bodyMedium)
            }
        is SettingsViewModel.UiState.Error ->
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) { Text("返回") }
            }
        SettingsViewModel.UiState.Idle -> Unit
        is SettingsViewModel.UiState.Done -> Unit
        SettingsViewModel.UiState.ResetCompleted -> Unit
    }
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
