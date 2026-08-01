package matchsong.app.navigation

/**
 * M2.1-2 类型安全导航参数定义。
 *
 * 路由模板见 [Routes]；参数名常量在此单点定义，禁止散落字符串。
 * M10.6：详情页由 M2 占位（Fake 歌曲）切真实推荐数据（BUG-004），
 * 推荐项数据经导航参数传递（songId + 展示字段 + 反馈关联 resultId）。
 */
object NavArgs {
    const val SONG_ID = "songId"
    const val SONG_TITLE = "title"
    const val SONG_ARTIST = "artist"
    const val SCORE = "score"
    const val KEY_SHIFT = "keyShift"
    const val EXPLANATION = "explanation"
    const val RESULT_ID = "resultId"
}
