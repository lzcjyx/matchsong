package matchsong.core.audio.android

import android.content.Context
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import matchsong.core.audio.algorithm.VolumeMeter
import matchsong.core.audio.algorithm.throttledVolume
import matchsong.core.audio.api.AudioRecorder
import matchsong.core.audio.api.RecordingConfig
import matchsong.core.common.log.Logger
import matchsong.core.common.result.OperationResult
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
    private val fileManager: RecordingFileManager? = null,
    private val logger: Logger = AndroidLogLogger(),
) : RecordingPort {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _stateFlow = MutableStateFlow<RecordingState>(RecordingState.IDLE)
    override val stateFlow: StateFlow<RecordingState> = _stateFlow.asStateFlow()

    private val _volumeFlow = MutableSharedFlow<VolumeLevel>(extraBufferCapacity = 1)
    override val volumeFlow: SharedFlow<VolumeLevel> = _volumeFlow.asSharedFlow()

    /** BUG-013：倒计时剩余秒数（3→2→1；进入 RECORDING 后为 0）。 */
    private val _countdownSeconds = MutableStateFlow(0)
    override val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    internal val stateMachine = RecordingStateMachine()

    private var focusManager: AudioFocusManager? = null
    private var sessionJob: Job? = null
    private var startTimeMs: Long = 0
    private var config: RecordingConfig = RecordingConfig()

    /** 当前会话 PCM 输出流（M8.1-1：录音落盘 → 分析消费）。 */
    private var pcmSink: java.io.DataOutputStream? = null
    private var sessionId: String = ""
    private var pcmFile: java.io.File? = null

    /** 录音完成后的 WAV 文件（quality/analysis 消费，M8.1-1）。 */
    @Volatile
    var lastWavFile: java.io.File? = null
        private set

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
                // 倒计时 3s（FR-REC-2）；BUG-013：每秒发射剩余秒数供 UI 渲染 3→2→1
                stateMachine.onEvent(RecordingEvent.Prepared)
                _stateFlow.value = RecordingState.COUNTDOWN
                _countdownSeconds.value = stateMachine.countdownRemaining
                repeat(3) {
                    delay(1000)
                    stateMachine.onEvent(RecordingEvent.Tick)
                    _stateFlow.value = stateMachine.state
                    _countdownSeconds.value = stateMachine.countdownRemaining
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

    /** 复用 PCM 编码缓冲（M10.2：避免逐样本 writeByte 与重复分配，PLAN §16.2 顺序 1-2）。 */
    private var pcmEncodeBuffer: ByteArray = ByteArray(0)

    /** 把一帧 float 样本编码为 little-endian 16bit PCM 并批量写入（每 chunk 一次 write）。 */
    private fun writePcmChunk(samples: FloatArray) {
        val sink = pcmSink ?: return
        val bytesNeeded = samples.size * 2
        if (pcmEncodeBuffer.size < bytesNeeded) {
            pcmEncodeBuffer = ByteArray(bytesNeeded)
        }
        val buffer = pcmEncodeBuffer
        for (i in samples.indices) {
            val sample = (samples[i] * 32767).toInt().coerceIn(-32768, 32767)
            buffer[i * 2] = (sample and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        sink.write(buffer, 0, bytesNeeded)
    }

    private suspend fun collectFrames() {
        // M8.1-1：录音落盘（sessionId.pcm）→ 供质量/分析消费；音量节流输出（M3.6-1）
        openPcmSink()
        val volumeMeter = VolumeMeter() // M10.2：复用实例，避免每 chunk 分配
        recorder.frames
            .onEach { chunk -> writePcmChunk(chunk.samples) }
            .map { chunk -> volumeMeter.computeVolume(chunk) }
            .throttledVolume(THROTTLE_MS)
            .collect { level -> _volumeFlow.tryEmit(level) }
        closePcmSink()
    }

    /** 创建会话 PCM 文件（cacheDir/recordings/{sessionId}.pcm）。 */
    private fun openPcmSink() {
        try {
            sessionId = java.util.UUID.randomUUID().toString()
            val manager = fileManager
            if (manager != null) {
                when (val created = manager.createSessionFiles(sessionId)) {
                    is matchsong.core.common.result.OperationResult.Success -> {
                        pcmFile = created.data
                        pcmSink = java.io.DataOutputStream(pcmFile!!.outputStream())
                    }
                    is matchsong.core.common.result.OperationResult.Failure -> {
                        logger.e(TAG, "创建 PCM 文件失败：${created.error}")
                        pcmSink = null
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "打开 PCM 输出失败（录音继续但不落盘）", e)
            pcmSink = null
        }
    }

    private fun closePcmSink() {
        try {
            pcmSink?.flush()
            pcmSink?.close()
        } catch (e: Exception) {
            logger.e(TAG, "关闭 PCM 输出失败", e)
        } finally {
            pcmSink = null
        }
    }

    /** 录音完成：PCM → WAV（质量/分析消费）。 */
    private fun finalizeWav() {
        val manager = fileManager ?: return
        when (val result = manager.finalizeWav(sessionId)) {
            is OperationResult.Success -> lastWavFile = result.data
            is OperationResult.Failure ->
                logger.e(TAG, "WAV 封装失败：${result.error}")
        }
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
        // BUG-014 修复：先落盘（PCM 关闭 + WAV 封装）再宣布 COMPLETED——
        // 原顺序在 COMPLETED 发射后才 finalizeWav，UI 可能在 lastWavFile 就绪前
        // 读取到 null 并跳转 → 质量页白屏（真机复现）
        closePcmSink()
        finalizeWav()
        stateMachine.onEvent(RecordingEvent.Stopped)
        _stateFlow.value = RecordingState.COMPLETED
        focusManager?.abandonFocus()
    }

    private fun onFocusLost() {
        logger.i(TAG, "音频焦点丢失，录音中断")
        stop(interrupted = true)
    }

    /**
     * M9.2 删除当前会话的 .pcm/.wav（分析完成/取消/失败后调用，FR-PRIV-1/ACC-14）。
     *
     * 调用约定：仅允许在录音已停止（录音完成、分析消费结束、取消、失败或服务销毁）
     * 后调用——进行中会话的文件删除由调用方保证时序（AppNavHost 在分析完成/重录时调用，
     * [release] 在采集停止后调用）。幂等：无会话或文件不存在时无操作；
     * 删除失败记录安全错误日志（M9.2），不抛出。
     */
    fun cleanupSessionFiles() {
        val manager = fileManager ?: return
        if (sessionId.isEmpty()) return
        when (val result = manager.deleteSessionFiles(sessionId)) {
            is OperationResult.Success -> lastWavFile = null
            is OperationResult.Failure ->
                logger.e(TAG, "删除录音临时文件失败（安全错误）：${result.error}")
        }
    }

    fun release() {
        // BUG-017 修复：仅做幂等资源收尾。
        // ① 不得置空 instance / 取消 scope——服务销毁（stopSelf → onDestroy）后 UI 流程
        //    仍需读取 lastWavFile（AppNavHost onFinished），且单例需支持再次录音；
        // ② 不得删除会话文件——文件删除仅由分析流程钩子（ANALYZING Done / 重录）与
        //    启动残留清理负责（M9.2 误加导致录音刚完成文件即被删 → “录音文件不可用”）。
        recorder.stop()
        focusManager?.abandonFocus()
    }
}
