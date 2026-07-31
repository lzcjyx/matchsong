package matchsong.app.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.domain.port.SongRepository
import matchsong.domain.recommendation.GetFavoriteSongIdsUseCase
import matchsong.domain.recommendation.ToggleFavoriteUseCase
import javax.inject.Inject

/**
 * M8.3-2 收藏页 ViewModel（FR-HX-2 UI 侧）。
 *
 * 观察收藏歌曲 ID Flow（单一数据源），再经 [SongRepository] 补全歌曲信息
 * （标题/歌手）；取消收藏即时从列表移除（Room Flow 自动发射）。
 */
@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        private val getFavoriteSongIds: GetFavoriteSongIdsUseCase,
        private val toggleFavorite: ToggleFavoriteUseCase,
        private val songRepository: SongRepository,
    ) : ViewModel() {
        /** 收藏列表 UI 状态。 */
        sealed interface UiState {
            /** 首次加载（Room 初值发射前，通常瞬态）。 */
            data object Loading : UiState

            /** 暂无收藏。 */
            data object Empty : UiState

            /** 收藏歌曲列表（按收藏时间倒序）。 */
            data class Content(val songs: List<FavoriteSong>) : UiState
        }

        /** 收藏列表条目（Port SongInfo 最小字段）。 */
        data class FavoriteSong(
            val songId: String,
            val title: String,
            val artist: String,
        )

        private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                getFavoriteSongIds().collect { ids ->
                    val songs =
                        ids.mapNotNull { id ->
                            songRepository.getById(id)?.let {
                                FavoriteSong(songId = it.songId, title = it.title, artist = it.artist)
                            }
                        }
                    _uiState.value = if (songs.isEmpty()) UiState.Empty else UiState.Content(songs)
                }
            }
        }

        /** 取消收藏（列表内条目点红心）。 */
        fun onToggle(songId: String) {
            viewModelScope.launch { toggleFavorite(songId) }
        }
    }
