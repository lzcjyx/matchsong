package matchsong.app.feature.common

import matchsong.core.common.error.AppError

/**
 * M8.6-1 错误恢复框架（ARCHITECTURE.md §12.3，PLAN M8.6 十类场景）。
 *
 * AppError → 用户文案 + 恢复动作。所有错误经此映射（无空 catch，P9）。
 */
object ErrorRecoveryHandler {
    /** 错误 → 用户可见文案（字符串资源 key 由 UI 层解析，MVP 直接中文）。 */
    fun userMessage(error: AppError): String =
        when (error) {
            is AppError.PermissionError -> "需要麦克风权限才能录音，请在设置中开启"
            is matchsong.core.common.error.AppError.RecordingError ->
                when (error.reason) {
                    matchsong.core.common.error.AppError.RecordingError.Reason.MicBusy -> "麦克风被占用，请稍后再试"
                    matchsong.core.common.error.AppError.RecordingError.Reason.PermissionRevoked -> "麦克风权限被撤销，请重新授权"
                    matchsong.core.common.error.AppError.RecordingError.Reason.InitFailed -> "录音初始化失败，请重试"
                    else -> "录音失败，请重试"
                }
            is AppError.QualityError -> "录音质量不合格，请重新录制"
            is AppError.AnalysisError -> "声音分析失败，请重试"
            is matchsong.core.common.error.AppError.StorageError.NoSpace -> "存储空间不足，请清理后重试"
            is matchsong.core.common.error.AppError.StorageError -> "数据保存失败，请重试"
            is AppError.DatabaseError -> "数据读取失败，请重试"
            is AppError.UnknownError -> "出了点问题，请重试"
        }

    /** 错误 → 建议动作（UI 据此渲染按钮）。 */
    fun action(error: AppError): RecoveryAction =
        when (error) {
            is AppError.PermissionError -> RecoveryAction.OPEN_SETTINGS
            is AppError.RecordingError -> RecoveryAction.RETRY_RECORDING
            is AppError.QualityError -> RecoveryAction.RETRY_RECORDING
            is AppError.AnalysisError -> RecoveryAction.RETRY_ANALYSIS
            is matchsong.core.common.error.AppError.StorageError.NoSpace -> RecoveryAction.CLEAN_STORAGE
            else -> RecoveryAction.RETRY
        }

    enum class RecoveryAction {
        RETRY,
        RETRY_RECORDING,
        RETRY_ANALYSIS,
        OPEN_SETTINGS,
        CLEAN_STORAGE,
        GO_HOME,
    }
}
