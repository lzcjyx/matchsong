package matchsong.core.audio.android

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import matchsong.core.audio.algorithm.VolumeMeter
import matchsong.core.audio.algorithm.throttledVolume
import matchsong.core.audio.api.AudioRecorder
import matchsong.core.audio.api.RecordingConfig
import matchsong.domain.recording.RecordingEvent
import matchsong.domain.recording.RecordingFailureReason
import matchsong.domain.recording.RecordingPort
import matchsong.domain.recording.RecordingState
import matchsong.domain.recording.RecordingStateMachine
import matchsong.domain.recording.VolumeLevel

/**
 * M3.2 录音会话执行器（单例，服务与 AndroidRecordingPort 共享）。
 *
 * 职责：驱动 [AudioRecorder] 采集、维护 [RecordingStateMachine]、音频焦点、
 * 音量流节流输出、自动停止（20s，ACC-4）。
 *
 * 线程模型：录音采集在 AudioRecorder 内部线程；本类用 CoroutineScope(Default) 驱动状态。
 * 实例经 Hilt 以 @Singleton 注入（app/di），`instance` 静态引用供 Service 无 DI 场景使用。
 */
class RecordingSessionRunner(
    private val recorder: AudioRecorder,
) : RecordingPort {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _stateFlow = MutableStateFlow<RecordingState>(RecordingState.IDLE)
    override val stateFlow: StateFlow<RecordingState> = _stateFlow.asStateFlow()

    private val _volumeFlow = MutableSharedFlow<VolumeLevel>(extraBufferCapacity = 1)
    override val volumeFlow: SharedFlow<VolumeLevel> = _volumeFlow.asSharedFlow()

    internal val stateMachine = RecordingStateMachine()

    private var focusManager: AudioFocusManager? = null
    private var sessionJob: Job? = null
    private var startTimeMs: Long = 0
    private var config: RecordingConfig = RecordingConfig()

    /** 静态引用：供 Service 无 DI 场景访问（app 装配时由 Hilt 设置）。 */
    companion object {
        @Volatile
        var instance: RecordingSessionRunner? = null

        const val TAG = "RecordingSession"
        const val THROTTLE_MS = 100L // 音量节流 ≤10Hz（FR-REC-4）
    }

    fun start(context: Context) {
        if (stateMachine.state != RecordingState.IDLE) return
        config = RecordingConfig() // M3.3-1 细化后从配置源读取
        stateMachine.onEvent(RecordingEvent.Start)
        _stateFlow.value = RecordingState.PREPARING

        // 焦点占用则不开始（MicBusy）
        focusManager = AudioFocusManager(context) { onFocusLost() }
        if (!focusManager!!.requestFocus()) {
            stateMachine.onEvent(RecordingEvent.Error(RecordingFailureReason.MIC_BUSY))
            _stateFlow.value = RecordingState.FAILED
            focusManager?.abandonFocus()
            return
        }

        sessionJob =
            scope.launch {
                // 倒计时 3s（FR-REC-2）
                stateMachine.onEvent(RecordingEvent.Prepared)
                _stateFlow.value = RecordingState.COUNTDOWN
                repeat(3) {
                    delay(1000)
                    stateMachine.onEvent(RecordingEvent.Tick)
                    _stateFlow.value = stateMachine.state
                }
                // 开始采集
                stateMachine.onEvent(RecordingEvent.RecordingStarted)
                _stateFlow.value = RecordingState.RECORDING
                startTimeMs = System.currentTimeMillis()
                collectFrames()
                // 自动停止 20s（ACC-4）
                delay(config.maxDurationMs - 3_000)
                stop(interrupted = false)
            }
    }

    private suspend fun collectFrames() {
        // 音量计算与节流统一走集中配置（M3.6-1，data-model §5.1；禁止散落阈值）
        recorder.frames
            .map { chunk -> VolumeMeter().computeVolume(chunk) }
            .throttledVolume(THROTTLE_MS)
            .collect { level -> _volumeFlow.tryEmit(level) }
    }

    override fun startRecording() {
        // UI 经 Port 触发（Context 由服务提供）；实际启动由服务 onStartCommand 完成
        // 本方法由 AndroidRecordingPort 在服务已启动后调用
    }

    override fun stopRecording() {
        stop(interrupted = false)
    }

    fun stop(interrupted: Boolean) {
        if (stateMachine.state == RecordingState.IDLE ||
            stateMachine.state == RecordingState.COMPLETED ||
            stateMachine.state == RecordingState.FAILED
        ) {
            return
        }
        if (interrupted) {
            stateMachine.onEvent(RecordingEvent.FocusLost)
        } else {
            stateMachine.onEvent(RecordingEvent.UserStop)
        }
        _stateFlow.value = RecordingState.STOPPING
        recorder.stop()
        sessionJob?.cancel()
        stateMachine.onEvent(RecordingEvent.Stopped)
        _stateFlow.value = RecordingState.COMPLETED
        focusManager?.abandonFocus()
    }

    private fun onFocusLost() {
        Log.i(TAG, "音频焦点丢失，录音中断")
        stop(interrupted = true)
    }

    fun release() {
        scope.cancel()
        recorder.stop()
        focusManager?.abandonFocus()
        instance = null
    }
}
