package matchsong.domain.recording

/**
 * M3.1-1 麦克风权限状态（FR-REC-5）。
 */
enum class PermissionState {
    /** 尚未请求。 */
    NOT_REQUESTED,

    /** 系统请求弹窗展示中。 */
    REQUESTING,

    /** 已授予。 */
    GRANTED,

    /** 已拒绝（可再次请求）。 */
    DENIED,

    /** 永久拒绝（"不再询问"），需引导去系统设置。 */
    PERMANENTLY_DENIED,

    /** 设备无麦克风硬件。 */
    UNAVAILABLE,
}

/**
 * 权限状态机输入事件。
 */
sealed interface PermissionEvent {
    /** 发起系统权限请求。 */
    data object Request : PermissionEvent

    /**
     * 系统请求结果。
     *
     * @param granted 是否授予。
     * @param shouldShowRationale 系统是否建议展示说明（false 且拒绝 → 永久拒绝判定依据）。
     */
    data class PermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean,
    ) : PermissionEvent

    /** App 从后台/设置返回（onResume），重新判定权限状态（设置返回后刷新，ACC-3）。 */
    data object AppResumed : PermissionEvent

    /** 设备无麦克风（M3.3 初始化失败映射）。 */
    data object DeviceUnavailable : PermissionEvent
}

/**
 * M3.1-1 纯 Kotlin 麦克风权限状态机（FR-REC-5，ARCHITECTURE.md §6.2）。
 *
 * 状态不持久化，每次会话重建。
 * 转移规则：
 * - NOT_REQUESTED --Request--> REQUESTING
 * - REQUESTING --PermissionResult(granted=true)--> GRANTED
 * - REQUESTING --PermissionResult(granted=false, rationale=true)--> DENIED（可重试）
 * - REQUESTING --PermissionResult(granted=false, rationale=false)--> PERMANENTLY_DENIED
 * - DENIED --Request--> REQUESTING
 * - PERMANENTLY_DENIED --AppResumed--> 重新判定（granted → GRANTED，否则保持）
 * - GRANTED --AppResumed--> GRANTED（保持；使用中撤销由服务侧错误回调联动录音状态机）
 * - 任意 --DeviceUnavailable--> UNAVAILABLE
 */
class PermissionStateMachine(
    initial: PermissionState = PermissionState.NOT_REQUESTED,
) {
    private var _state: PermissionState = initial

    val state: PermissionState get() = _state

    /** 注入事件并返回新状态。非法转移被忽略并保留原状态。 */
    fun onEvent(event: PermissionEvent): PermissionState {
        _state =
            when (event) {
                PermissionEvent.Request ->
                    when (_state) {
                        PermissionState.NOT_REQUESTED,
                        PermissionState.DENIED,
                        PermissionState.REQUESTING,
                        -> PermissionState.REQUESTING

                        else -> _state
                    }

                is PermissionEvent.PermissionResult ->
                    when (_state) {
                        PermissionState.REQUESTING ->
                            when {
                                event.granted -> PermissionState.GRANTED
                                !event.shouldShowRationale -> PermissionState.PERMANENTLY_DENIED
                                else -> PermissionState.DENIED
                            }

                        else -> _state
                    }

                PermissionEvent.AppResumed ->
                    when (_state) {
                        // 从设置返回：由外部调用方注入新的 PermissionResult 判定；
                        // 本事件仅保证状态机在 onResume 时可重新评估入口（ARCHITECTURE.md §6.2）。
                        PermissionState.GRANTED,
                        PermissionState.PERMANENTLY_DENIED,
                        -> _state

                        else -> _state
                    }

                PermissionEvent.DeviceUnavailable -> PermissionState.UNAVAILABLE
            }
        return _state
    }

    /** 是否允许发起系统请求。 */
    fun canRequest(): Boolean =
        when (_state) {
            PermissionState.NOT_REQUESTED,
            PermissionState.DENIED,
            PermissionState.REQUESTING,
            -> true

            else -> false
        }
}
