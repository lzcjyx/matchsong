package matchsong.core.common.error

/**
 * 应用错误层级（ARCHITECTURE.md §12.2，M1.4-1）。
 *
 * 每个错误携带：
 * - [messageKey]：面向用户的文案 key（如 "error.permission.denied"），本地化文案由 feature 层资源提供；
 * - [cause]：根因异常（可空，排障用）；
 * - [details]：附加结构化细节（可空 Map）。
 *
 * 规则（P9）：禁止空 catch；每个 catch 必须记录日志并映射为类型化错误；
 * 未预期异常包 [UnknownError] 并保留堆栈。
 */
sealed class AppError(
    val messageKey: String,
    val cause: Throwable? = null,
    val details: Map<String, String> = emptyMap(),
) {
    /** 麦克风权限相关错误（FR-REC-5）。 */
    sealed class PermissionError(
        messageKey: String,
        cause: Throwable? = null,
        details: Map<String, String> = emptyMap(),
    ) : AppError(messageKey, cause, details) {
        /** 尚未请求权限。 */
        object NotRequested : PermissionError("error.permission.not_requested")

        /** 用户拒绝（可再次请求）。 */
        object Denied : PermissionError("error.permission.denied")

        /** 永久拒绝（shouldShowRationale=false，引导去系统设置）。 */
        object PermanentlyDenied : PermissionError("error.permission.permanently_denied")

        /** 设备无麦克风。 */
        object Unavailable : PermissionError("error.permission.unavailable")
    }

    /** 录音过程错误（§6.2 RecordingStateMachine Failed 分支）。 */
    sealed class RecordingError(
        messageKey: String,
        cause: Throwable? = null,
        details: Map<String, String> = emptyMap(),
        val reason: Reason = Reason.Unknown,
    ) : AppError(messageKey, cause, details) {
        /** 失败原因分类（M3.3-3 错误映射/采样率降级，供 UI 与状态机区分处理）。 */
        enum class Reason {
            /** 初始化失败（采样率探测全部失败 / AudioRecord 未初始化）。 */
            InitFailed,

            /** 设备无可用麦克风。 */
            MicUnavailable,

            /** 麦克风被其他应用占用（含焦点获取失败，§6.2）。 */
            MicBusy,

            /** 采集期间 PCM 读取失败。 */
            ReadError,

            /** 录音期间麦克风权限被撤销。 */
            PermissionRevoked,

            /** 未知原因（兜底）。 */
            Unknown,
        }

        /** 初始化失败（无麦克风/被占用/读取失败），[cause] 携带具体根因。 */
        class InitFailed(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : RecordingError("error.recording.init_failed", cause, details, Reason.InitFailed)

        /** 设备无可用麦克风（无麦克风设备检测，M3.3-3）。 */
        class MicUnavailable(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : RecordingError("error.recording.mic_unavailable", cause, details, Reason.MicUnavailable)

        /** 麦克风被其他应用占用（含焦点获取失败，ARCHITECTURE.md §6.2）。 */
        class MicBusy(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : RecordingError("error.recording.mic_busy", cause, details, Reason.MicBusy)

        /** 采集期间 PCM 读取失败（read() 返回错误码或抛出异常）。 */
        class ReadError(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : RecordingError("error.recording.read_error", cause, details, Reason.ReadError)

        /** 录音期间麦克风权限被撤销。 */
        class PermissionRevoked(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : RecordingError("error.recording.permission_revoked", cause, details, Reason.PermissionRevoked)

        /** 被来电等中断（焦点丢失，ADR-002 遗留项）。 */
        object Interrupted : RecordingError("error.recording.interrupted")

        /** 用户取消。 */
        object Canceled : RecordingError("error.recording.canceled")
    }

    /** 质量门禁失败（FR-QUAL-3，ACC-7/8）。 */
    sealed class QualityError(
        messageKey: String,
        cause: Throwable? = null,
        details: Map<String, String> = emptyMap(),
    ) : AppError(messageKey, cause, details) {
        /** 录音过短。 */
        object TooShort : QualityError("error.quality.too_short")

        /** 未检测到声音。 */
        object Silent : QualityError("error.quality.silent")

        /** 音量过小。 */
        object TooQuiet : QualityError("error.quality.too_quiet")

        /** 环境嘈杂。 */
        object Noisy : QualityError("error.quality.noisy")

        /** 麦克风削波。 */
        object Clipping : QualityError("error.quality.clipping")

        /** 有效演唱片段不足（FR-QUAL-3）。 */
        object InsufficientValidFrames : QualityError("error.quality.insufficient_valid_frames")
    }

    /** 分析过程错误（§9 分析流水线）。 */
    sealed class AnalysisError(
        messageKey: String,
        cause: Throwable? = null,
        details: Map<String, String> = emptyMap(),
    ) : AppError(messageKey, cause, details) {
        /** 分析被取消（协程取消不包装为业务错误，本类型仅用于显式取消路径）。 */
        object Canceled : AnalysisError("error.analysis.canceled")

        /** 有效演唱片段不足，不输出正式结果（FR-ANAL-8，ACC-9）。 */
        object InsufficientData : AnalysisError("error.analysis.insufficient_data")

        /** 内部错误（未预期异常，必须记录堆栈）。 */
        class Internal(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : AnalysisError("error.analysis.internal", cause, details)
    }

    /** 存储错误（文件缓存/磁盘）。 */
    sealed class StorageError(
        messageKey: String,
        cause: Throwable? = null,
        details: Map<String, String> = emptyMap(),
    ) : AppError(messageKey, cause, details) {
        /** 空间不足。 */
        object NoSpace : StorageError("error.storage.no_space")

        /** 文件读写失败，[cause] 携带具体根因。 */
        class Io(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : StorageError("error.storage.io", cause, details)

        /** 音频文件损坏。 */
        object CorruptFile : StorageError("error.storage.corrupt_file")
    }

    /** 数据库错误（Room/DataStore）。 */
    sealed class DatabaseError(
        messageKey: String,
        cause: Throwable? = null,
        details: Map<String, String> = emptyMap(),
    ) : AppError(messageKey, cause, details) {
        class Query(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : DatabaseError("error.database.query", cause, details)

        class Insert(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : DatabaseError("error.database.insert", cause, details)

        class Corrupt(
            cause: Throwable? = null,
            details: Map<String, String> = emptyMap(),
        ) : DatabaseError("error.database.corrupt", cause, details)
    }

    /** 兜底错误：未预期异常，必须记录堆栈（P9）。 */
    class UnknownError(
        cause: Throwable? = null,
        details: Map<String, String> = emptyMap(),
    ) : AppError("error.unknown", cause, details)

    companion object {
        /**
         * 错误 → 用户文案 key 的映射表（ARCHITECTURE.md §12.3 示例文案）。
         * [messageKey] 是稳定契约；此处中文文案为**开发期示例**，正式本地化文案由 feature 层字符串资源提供。
         */
        val EXAMPLE_USER_MESSAGES: Map<String, String> =
            mapOf(
                "error.permission.not_requested" to "尚未请求麦克风权限",
                "error.permission.denied" to "需要麦克风权限才能测试",
                "error.permission.permanently_denied" to "请在系统设置中开启麦克风",
                "error.permission.unavailable" to "设备没有可用的麦克风",
                "error.recording.init_failed" to "无法开始录音（麦克风不可用或被占用）",
                "error.recording.mic_unavailable" to "设备没有可用的麦克风",
                "error.recording.mic_busy" to "麦克风正被其他应用使用",
                "error.recording.read_error" to "录音数据读取失败",
                "error.recording.permission_revoked" to "麦克风权限已撤销，无法继续录音",
                "error.recording.interrupted" to "录音被来电中断，本次结果可能不完整",
                "error.recording.canceled" to "录音已取消",
                "error.quality.too_short" to "录音过短，请至少演唱 10 秒",
                "error.quality.silent" to "没有检测到声音",
                "error.quality.too_quiet" to "声音太小，请靠近麦克风",
                "error.quality.noisy" to "环境嘈杂，请到安静环境重录",
                "error.quality.clipping" to "麦克风削波，请降低音量",
                "error.quality.insufficient_valid_frames" to "有效演唱片段不足，请重录",
                "error.analysis.canceled" to "分析已取消",
                "error.analysis.insufficient_data" to "有效演唱片段不足，请重录",
                "error.analysis.internal" to "分析失败，请重试",
                "error.storage.no_space" to "存储空间不足",
                "error.storage.io" to "文件读写失败",
                "error.storage.corrupt_file" to "音频文件损坏",
                "error.database.query" to "数据读取失败",
                "error.database.insert" to "数据保存失败",
                "error.database.corrupt" to "本地数据损坏",
                "error.unknown" to "出错了，请重试",
            )
    }
}
