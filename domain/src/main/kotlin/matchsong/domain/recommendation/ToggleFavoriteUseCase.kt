package matchsong.domain.recommendation

import matchsong.domain.port.FavoritesRepository

/**
 * 收藏切换用例（M8.3-1，FR-HX-2）。
 *
 * 详情页/收藏页共用的唯一写入入口：已收藏 → 取消，未收藏 → 收藏（幂等）。
 * 收藏状态变更经 FavoritesRepository Flow 自动同步到各订阅方。
 */
class ToggleFavoriteUseCase(
    private val favoritesRepository: FavoritesRepository,
) {
    suspend operator fun invoke(songId: String) {
        favoritesRepository.toggle(songId)
    }
}
