package matchsong.core.audio.android

import matchsong.core.common.error.AppError

/**
 * 采集阶段（M3.3-3）：决定同一异常在不同阶段的映射方向。
 */
enum class AudioCaptureStage {
    /** AudioRecord 构造与初始化阶段（含采样率探测）。 */
    INIT,

    /** [android.media.AudioRecord.startRecording] 启动阶段。 */
    START,

    /** 采集循环 read() 阶段。 */
    READ,
}

/**
 * AudioRecord 异常 → 类型化 [AppError] 映射（M3.3-3，ARCHITECTURE.md §12.2/P9）。
 *
 * 纯函数、无 Android 依赖，JVM 可直接测试：
 * - [SecurityException] → PermissionRevoked（权限缺失/录音中撤销）；
 * - [IllegalArgumentException] → InitFailed（INIT）/ ReadError（READ）；
 * - [IllegalStateException] → InitFailed（INIT）/ MicBusy（START，麦克风被占用）/ ReadError（READ）；
 * - 其余未预期异常 → UnknownError（P9 兜底，保留堆栈）。
 *
 * 规则（P9）：禁止空 catch——调用方每个 catch 必须记录日志并映射为类型化错误，
 * 未预期异常包 [AppError.UnknownError] 并保留堆栈。
 */
object RecordingErrorMapper {
    /**
     * 映射单个异常。
     *
     * @param throwable 捕获到的异常（禁止空 catch 吞掉）。
     * @param stage 异常发生的采集阶段。
     * @return 映射后的类型化错误（RecordingError 各子类或 UnknownError 兜底）。
     */
    fun map(
        throwable: Throwable,
        stage: AudioCaptureStage,
    ): AppError =
        when (throwable) {
            is SecurityException ->
                AppError.RecordingError.PermissionRevoked(cause = throwable)

            is IllegalArgumentException ->
                when (stage) {
                    AudioCaptureStage.INIT -> AppError.RecordingError.InitFailed(cause = throwable)
                    else -> AppError.RecordingError.ReadError(cause = throwable)
                }

            is IllegalStateException ->
                when (stage) {
                    AudioCaptureStage.INIT -> AppError.RecordingError.InitFailed(cause = throwable)
                    AudioCaptureStage.START -> AppError.RecordingError.MicBusy(cause = throwable)
                    AudioCaptureStage.READ -> AppError.RecordingError.ReadError(cause = throwable)
                }

            else -> AppError.UnknownError(cause = throwable)
        }

    /**
     * 初始化失败工厂：采样率探测全部失败 / AudioRecord 未初始化状态等场景。
     *
     * @param cause 具体根因（排障用）。
     * @param details 附加细节（如 attemptedRates 降级探测记录）。
     */
    fun initFailed(
        cause: Throwable?,
        details: Map<String, String> = emptyMap(),
    ): AppError.RecordingError.InitFailed = AppError.RecordingError.InitFailed(cause = cause, details = details)
}
