package matchsong.app.feature.recommendation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.AppTopBar
import matchsong.app.design.components.SongCard
import matchsong.app.design.components.state.EmptyState

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
 * M10.6 推荐详情页（BUG-004：由 M2 占位 Fake 数据切换为真实推荐项数据）。
 *
 * 数据经导航参数传递（[Routes.recommendationDetail]）：歌曲名/歌手/匹配度/
 * 变调建议/推荐理由/推荐结果 ID（反馈关联）。反馈入口（FR-HX-3，BUG-001）：
 * 六类反馈经 [FeedbackSheet] 提交，仅保存不调权重。
 */
@Composable
fun RecommendationDetailScreen(
    songId: String,
    title: String,
    artist: String,
    score: Double?,
    keyShiftSemitones: Int?,
    explanation: String?,
    resultId: String?,
    onBack: () -> Unit,
    viewModel: RecommendationDetailViewModel = hiltViewModel(),
) {
    var showFeedback by remember { mutableStateOf(false) }
    val submitted by viewModel.submitted.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        AppTopBar(title = "歌曲详情", onBack = onBack)
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = artist, style = MaterialTheme.typography.titleMedium)
        score?.let { s ->
            Text(
                text = "匹配度 ${s.toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = MatchSongSpacing.M),
            )
        }
        keyShiftSemitones?.let { shift ->
            Text(
                text = if (shift < 0) "建议降 ${-shift} 半音" else "建议升 $shift 半音",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (!explanation.isNullOrBlank()) {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = MatchSongSpacing.M),
            )
        }
        Text(
            text = "* 本次录音估计，非专业诊断",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = MatchSongSpacing.M),
        )
        Button(
            onClick = { showFeedback = true },
            modifier = Modifier.padding(top = MatchSongSpacing.L),
            enabled = !submitted,
        ) {
            Text(if (submitted) "已收到反馈，谢谢" else "反馈推荐结果")
        }
    }

    if (showFeedback) {
        FeedbackSheet(
            onDismiss = { showFeedback = false },
            onSubmit = { type ->
                viewModel.submitFeedback(songId = songId, resultId = resultId, type = type)
                showFeedback = false
            },
        )
    }
}
