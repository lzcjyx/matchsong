package matchsong.domain.recording

/**
 * M3.4-1 录音状态（FR-REC-6，MVP 无 Pause）。
 */
enum class RecordingState {
    IDLE,
    PREPARING,
    COUNTDOWN,
    RECORDING,
    STOPPING,
    COMPLETED,
    FAILED,
}

/**
 * M3.4-1 录音会话事件。
 */
sealed interface RecordingEvent {
    /** 用户触发开始（进入准备）。 */
    data object Start : RecordingEvent

    /** 倒计时开始（Preparing → Countdown）。 */
    data object Prepared : RecordingEvent

    /** 倒计时 tick（每秒）。 */
    data object Tick : RecordingEvent

    /** 倒计时结束，开始采集。 */
    data object RecordingStarted : RecordingEvent

    /** 用户提前停止。 */
    data object UserStop : RecordingEvent

    /** 达到自动停止时长（20s 默认，ACC-4）。 */
    data object AutoStop : RecordingEvent

    /** 音频焦点丢失（来电等，M3.2-3）。 */
    data object FocusLost : RecordingEvent

    /** 致命错误（初始化失败/无麦克风/读取错误/权限撤销）。 */
    data class Error(val reason: RecordingFailureReason) : RecordingEvent

    /** 采集结束（内部确认，进入 Completed）。 */
    data object Stopped : RecordingEvent
}

/**
 * 录音失败原因（M3.3-3 错误映射 + 权限联动）。
 */
enum class RecordingFailureReason {
    INIT_FAILED,
    MIC_UNAVAILABLE,
    MIC_BUSY,
    READ_ERROR,
    PERMISSION_REVOKED,
    INTERRUPTED,
    CANCELED,
    UNKNOWN,
}

/**
 * M3.4-1 纯 Kotlin 录音状态机（FR-REC-6，ARCHITECTURE.md §6.2）。
 *
 * 状态转移：
 * IDLE --Start--> PREPARING
 * PREPARING --Prepared--> COUNTDOWN
 * COUNTDOWN --Tick(n 次)--> COUNTDOWN（3s 倒计时）
 * COUNTDOWN --RecordingStarted--> RECORDING
 * RECORDING --UserStop/AutoStop/FocusLost--> STOPPING
 * RECORDING --Error--> FAILED
 * STOPPING --Stopped--> COMPLETED（interrupted=true 当 FocusLost 触发）
 * 任意 --Error--> FAILED（可中断路径）
 *
 * 非法事件被忽略并保留原状态。
 */
class RecordingStateMachine(
    private val countdownTicks: Int = 3,
) {
    private var stateInternal: RecordingState = RecordingState.IDLE
    private var tickCount: Int = 0
    private var interruptedFlag: Boolean = false
    private var failureReasonInternal: RecordingFailureReason? = null

    val state: RecordingState get() = stateInternal
    val interrupted: Boolean get() = interruptedFlag
    val failureReason: RecordingFailureReason? get() = failureReasonInternal
    val countdownRemaining: Int get() = (countdownTicks - tickCount).coerceAtLeast(0)

    fun onEvent(event: RecordingEvent): RecordingState {
        when (event) {
            RecordingEvent.Start ->
                if (stateInternal == RecordingState.IDLE ||
                    stateInternal == RecordingState.COMPLETED ||
                    stateInternal == RecordingState.FAILED
                ) {
                    stateInternal = RecordingState.PREPARING
                    interruptedFlag = false
                    failureReasonInternal = null
                    tickCount = 0
                }

            RecordingEvent.Prepared ->
                if (stateInternal == RecordingState.PREPARING) {
                    stateInternal = RecordingState.COUNTDOWN
                    tickCount = 0
                }

            RecordingEvent.Tick ->
                if (stateInternal == RecordingState.COUNTDOWN) {
                    tickCount++
                    if (tickCount >= countdownTicks) {
                        stateInternal = RecordingState.RECORDING
                    }
                }

            RecordingEvent.RecordingStarted ->
                if (stateInternal == RecordingState.COUNTDOWN || stateInternal == RecordingState.RECORDING) {
                    stateInternal = RecordingState.RECORDING
                }

            RecordingEvent.UserStop,
            RecordingEvent.AutoStop,
            ->
                if (stateInternal == RecordingState.RECORDING) {
                    stateInternal = RecordingState.STOPPING
                }

            RecordingEvent.FocusLost ->
                if (stateInternal == RecordingState.RECORDING) {
                    stateInternal = RecordingState.STOPPING
                    interruptedFlag = true
                }

            is RecordingEvent.Error ->
                if (stateInternal != RecordingState.COMPLETED && stateInternal != RecordingState.FAILED) {
                    stateInternal = RecordingState.FAILED
                    failureReasonInternal = event.reason
                }

            RecordingEvent.Stopped ->
                if (stateInternal == RecordingState.STOPPING) {
                    stateInternal = RecordingState.COMPLETED
                }
        }
        return stateInternal
    }

    /** 会话是否处于可中断的活跃状态（用于文件清理判断）。 */
    fun isActive(): Boolean =
        when (stateInternal) {
            RecordingState.PREPARING,
            RecordingState.COUNTDOWN,
            RecordingState.RECORDING,
            RecordingState.STOPPING,
            -> true

            else -> false
        }
}
