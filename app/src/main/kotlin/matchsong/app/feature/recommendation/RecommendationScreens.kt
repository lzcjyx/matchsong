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

/** M2.4-2 测试数据（M7 推荐引擎接入后替换；debug 构建明确标记）。 */
data class FakeSong(val id: String, val title: String, val artist: String, val note: String)

val FakeSongs =
    listOf(
        FakeSong("song-1", "晴天", "周杰伦", "推荐理由：大部分旋律位于你的舒适音区"),
        FakeSong("song-2", "小幸运", "田馥甄", "推荐理由：原调最高音略高，降低 2 个半音后适合"),
        FakeSong("song-3", "平凡之路", "朴树", "推荐理由：持续高音较少，适合当前稳定性"),
    )

/**
 * M7 推荐列表页（M2 阶段用 Fake 数据演示全流程）。
 */
@Composable
fun RecommendationListScreen(
    onSongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "推荐歌曲")
        Text(
            text = "测试数据（M2 演示，M7 接入真实推荐引擎）",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = MatchSongSpacing.M),
        )
        if (FakeSongs.isEmpty()) {
            EmptyState(text = "暂无匹配的歌曲", actionText = "重新测试", onAction = {})
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(MatchSongSpacing.M),
                verticalArrangement = Arrangement.spacedBy(MatchSongSpacing.S),
            ) {
                items(FakeSongs, key = { it.id }) { song ->
                    SongCard(
                        title = song.title,
                        subtitle = "${song.artist} · ${song.note}",
                        onClick = { onSongClick(song.id) },
                    )
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
