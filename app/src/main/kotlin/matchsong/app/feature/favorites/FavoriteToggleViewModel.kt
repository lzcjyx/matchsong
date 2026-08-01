package matchsong.app.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import matchsong.domain.recommendation.GetFavoriteSongIdsUseCase
import matchsong.domain.recommendation.ToggleFavoriteUseCase
import javax.inject.Inject

/**
 * M8.3-2 详情页收藏按钮 ViewModel（FR-HX-2 UI 侧）。
 *
 * 收藏态经 GetFavoriteSongIdsUseCase 的 Flow 实时同步（单一数据源，
 * 与收藏页状态一致）；初始值 StateFlow(false) 冷启动不闪烁（M8.3-2 风险项）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoriteToggleViewModel
    @Inject
    constructor(
        private val toggleFavorite: ToggleFavoriteUseCase,
        private val getFavoriteSongIds: GetFavoriteSongIdsUseCase,
    ) : ViewModel() {
        private val songIdFlow = MutableStateFlow<String?>(null)

        /** 当前歌曲是否已收藏（Flow 实时同步）。 */
        val favoriteState: StateFlow<Boolean> =
            songIdFlow
                .flatMapLatest { songId ->
                    if (songId == null) flowOf(false) else getFavoriteSongIds().map { it.contains(songId) }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    initialValue = false,
                )

        /** 绑定目标歌曲（进入详情页时调用；songId 变化自动重订阅）。 */
        fun setSong(songId: String) {
            songIdFlow.value = songId
        }

        /** 收藏/取消收藏。 */
        fun toggle() {
            val songId = songIdFlow.value ?: return
            viewModelScope.launch { toggleFavorite(songId) }
        }
    }
