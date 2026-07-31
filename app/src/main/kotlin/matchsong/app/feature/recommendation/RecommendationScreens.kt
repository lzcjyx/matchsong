package matchsong.app.feature.recommendation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.AppTopBar
import matchsong.app.design.components.SongCard
import matchsong.app.design.components.state.EmptyState

/** M2.4-2 测试数据（详情页演示用；推荐列表已接真实引擎 M8.2-1）。 */
data class FakeSong(val id: String, val title: String, val artist: String, val note: String)

val FakeSongs =
    listOf(
        FakeSong("song-1", "晴天", "周杰伦", "推荐理由：大部分旋律位于你的舒适音区"),
        FakeSong("song-2", "小幸运", "田馥甄", "推荐理由：原调最高音略高，降低 2 个半音后适合"),
        FakeSong("song-3", "平凡之路", "朴树", "推荐理由：持续高音较少，适合当前稳定性"),
    )

/**
 * M8.2-1 推荐列表页（真实引擎数据：分数 + 解释 + 变调建议，ACC-11）。
 */
@Composable
fun RecommendationListScreen(
    state: RecommendationListViewModel.UiState,
    onSongClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "推荐歌曲")
        when (state) {
            RecommendationListViewModel.UiState.Idle -> Unit
            RecommendationListViewModel.UiState.Loading ->
                matchsong.app.design.components.state.LoadingState(text = "正在生成推荐…")
            is RecommendationListViewModel.UiState.Error ->
                matchsong.app.design.components.state.ErrorState(
                    message = state.message,
                    onRetry = onRetry,
                )
            is RecommendationListViewModel.UiState.Success -> {
                val result = state.result
                if (result.recommendations.isEmpty()) {
                    EmptyState(
                        text = result.emptyStateReason ?: "暂无匹配的歌曲",
                        actionText = "重新测试",
                        onAction = onRetry,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(MatchSongSpacing.M),
                        verticalArrangement = Arrangement.spacedBy(MatchSongSpacing.S),
                    ) {
                        items(result.recommendations, key = { it.song.songId }) { item ->
                            SongCard(
                                title = item.song.title,
                                subtitle =
                                    buildString {
                                        append(item.song.artist)
                                        append(" · 匹配度 ${item.score.toInt()}%")
                                        item.keyShiftSemitones?.let { shift ->
                                            append(if (shift < 0) " · 建议降 ${-shift} 半音" else " · 建议升 $shift 半音")
                                        }
                                    },
                                onClick = { onSongClick(item.song.songId) },
                            ) {
                                Text(
                                    text = item.explanation.firstOrNull() ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * M7 推荐详情页（M2 占位）。songId 经导航参数传入。
 */
@Composable
fun RecommendationDetailScreen(
    songId: String,
    onBack: () -> Unit,
) {
    val song = FakeSongs.firstOrNull { it.id == songId }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        AppTopBar(title = "歌曲详情", onBack = onBack)
        if (song == null) {
            EmptyState(text = "未找到该歌曲", actionText = "返回", onAction = onBack)
        } else {
            Text(text = song.title, style = MaterialTheme.typography.headlineMedium)
            Text(text = song.artist, style = MaterialTheme.typography.titleMedium)
            Text(
                text = song.note,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = MatchSongSpacing.M),
            )
            Text(
                text = "变调建议：-2 半音（示例）\n* 本次录音估计，非专业诊断",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = MatchSongSpacing.M),
            )
        }
    }
}
