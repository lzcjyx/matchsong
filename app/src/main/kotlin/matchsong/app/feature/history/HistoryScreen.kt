package matchsong.app.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.AppTopBar
import matchsong.app.design.components.SongCard
import matchsong.app.design.components.state.EmptyState
import matchsong.domain.port.AnalysisSummary

/**
 * M8.4-2 历史记录页（SPEC §4.2 历史记录，FR-HX-1）。
 *
 * 按记录时间倒序展示分析摘要（时间 + 稳定音域 + 置信度徽标）；点击条目进入
 * 结果/推荐（导航由调用方接线 [onHistoryClick]）；单条删除需确认（M9.3 UI 联动）。
 * 仅展示摘要，不含原始音频（FR-HX-1）。
 *
 * [onHistoryClick] 提供默认空实现：AppNavHost 尚未接线前页面可独立编译预览。
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onHistoryClick: (String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    var pendingDelete by remember { mutableStateOf<AnalysisSummary?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "历史记录", onBack = onBack)

        if (items.isEmpty()) {
            EmptyState(text = "暂无历史记录\n完成一次声音分析后，摘要会出现在这里")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(MatchSongSpacing.M),
                verticalArrangement = Arrangement.spacedBy(MatchSongSpacing.S),
            ) {
                items(items, key = { it.analysisId }) { item ->
                    SongCard(
                        title = item.timeText(),
                        subtitle =
                            "稳定音域 ${item.rangeSummaryText()} · 置信度：${item.confidenceLabelText()}",
                        onClick = { onHistoryClick(item.analysisId) },
                        trailing = {
                            TextButton(onClick = { pendingDelete = item }) { Text("删除") }
                        },
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条历史记录？") },
            text = { Text("将删除 ${item.timeText()} 的分析摘要。此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(item.analysisId)
                        pendingDelete = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}
