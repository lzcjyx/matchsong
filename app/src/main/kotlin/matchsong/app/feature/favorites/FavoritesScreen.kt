package matchsong.app.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.AppTopBar
import matchsong.app.design.components.SongCard
import matchsong.app.design.components.state.EmptyState
import matchsong.app.design.components.state.LoadingState

/**
 * M8.3-2 收藏页（FR-HX-2 UI 侧）。
 *
 * 收藏歌曲列表（标题/歌手，按收藏时间倒序）；点击条目回调 [onSongClick]
 * 导航详情页；空收藏展示 [EmptyState]；取消收藏后列表经 Flow 实时移除。
 *
 * @param onSongClick 歌曲点击回调（由导航层传入，跳转推荐详情页）。
 */
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onSongClick: (String) -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "我的收藏", onBack = onBack)
        when (val state = uiState) {
            is FavoritesViewModel.UiState.Loading -> LoadingState()
            is FavoritesViewModel.UiState.Empty -> EmptyState(text = "暂无收藏歌曲")
            is FavoritesViewModel.UiState.Content ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MatchSongSpacing.M),
                    verticalArrangement = Arrangement.spacedBy(MatchSongSpacing.S),
                ) {
                    items(state.songs, key = { it.songId }) { song ->
                        SongCard(
                            title = song.title,
                            subtitle = song.artist,
                            onClick = { onSongClick(song.songId) },
                        )
                    }
                }
        }
    }
}
