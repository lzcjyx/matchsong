package matchsong.core.common.log

/**
 * 日志接口（ARCHITECTURE.md §13，FR-PRIV-4，M1.4-3）。
 *
 * - tag 约定 `MatchSong:<Layer>`，见 [LogTags]；
 * - Android 实现 AndroidLogger（android.util.Log）在 app 层经 Hilt 注入；
 * - 测试用 TestLogger（记录调用供断言）。
 *
 * Release 构建下消息统一经 [LogRedactor] 脱敏（路径/设备标识/会话号），
 * 原始音频样本与内容在任何级别均禁止输出——这是调用方约定。
 */
interface Logger {
    /** 调试细节：仅 Debug 构建输出（R8 常量折叠移除 Release 调用）。 */
    fun d(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    /** 信息事件（含审计事件：同意记录、数据删除等）。 */
    fun i(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    /** 警告。 */
    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    /** 错误（必须保留脱敏消息与堆栈）。 */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

/** tag 约定（ARCHITECTURE.md §13）。 */
object LogTags {
    /** 录音 feature 层。 */
    const val RECORDING = "MatchSong:Rec"

    /** 音频引擎（core:audio）。 */
    const val AUDIO = "MatchSong:Audio"

    /** 分析域。 */
    const val ANALYSIS = "MatchSong:Analysis"

    /** 推荐域。 */
    const val RECOMMENDATION = "MatchSong:Recm"

    /** 数据层（Room/DataStore/文件）。 */
    const val DATA = "MatchSong:Data"
}
