package matchsong.app.navigation

/**
 * M2.1-1 路由表（FR-SHELL-1）。
 *
 * 单点定义，页面引用常量；禁止在业务代码中硬编码路由字符串。
 */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PREPARE = "prepare"
    const val RECORDING = "recording"
    const val QUALITY_RESULT = "quality_result"
    const val ANALYZING = "analyzing"
    const val VOICE_RESULT = "voice_result"
    const val RECOMMENDATION_LIST = "recommendation_list"
    const val RECOMMENDATION_DETAIL = "recommendation_detail/{songId}"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DELETE_CONFIRM = "delete_confirm"

    fun recommendationDetail(songId: String): String = "recommendation_detail/$songId"
}
