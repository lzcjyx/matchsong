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
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DELETE_CONFIRM = "delete_confirm"

    /** M10.6 推荐详情：真实推荐项数据经参数传递（BUG-004；feedback 关联 resultId）。 */
    const val RECOMMENDATION_DETAIL =
        "recommendation_detail/{songId}?title={title}&artist={artist}" +
            "&score={score}&keyShift={keyShift}&explanation={explanation}&resultId={resultId}"

    fun recommendationDetail(
        songId: String,
        title: String,
        artist: String,
        score: Int?,
        keyShift: Int?,
        explanation: String?,
        resultId: String?,
    ): String {
        val base =
            "recommendation_detail/$songId?title=${android.net.Uri.encode(title)}" +
                "&artist=${android.net.Uri.encode(artist)}"
        val query =
            buildString {
                score?.let { append("&score=$it") }
                keyShift?.let { append("&keyShift=$it") }
                if (!explanation.isNullOrBlank()) append("&explanation=${android.net.Uri.encode(explanation)}")
                resultId?.let { append("&resultId=${android.net.Uri.encode(it)}") }
            }
        return base + query
    }
}
