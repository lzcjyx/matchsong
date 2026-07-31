package matchsong.domain.recommendation

import kotlinx.coroutines.flow.Flow
import matchsong.domain.port.FavoritesRepository

/**
 * 收藏状态观察用例（M8.3-1，FR-HX-2）。
 *
 * 返回收藏歌曲 ID 集合 Flow（按收藏时间倒序）；订阅期间任何
 * 收藏/取消即时发射，供列表页与详情页按钮态保持单一数据源。
 */
class GetFavoriteSongIdsUseCase(
    private val favoritesRepository: FavoritesRepository,
) {
    operator fun invoke(): Flow<Set<String>> = favoritesRepository.observeFavoriteSongIds()
}
