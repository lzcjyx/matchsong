package matchsong.core.audio.android

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import matchsong.core.audio.api.AudioChunk
import matchsong.core.audio.api.AudioRecorder
import matchsong.core.audio.api.RecordingConfig
import matchsong.core.common.error.AppError
import matchsong.core.common.log.LogTags
import matchsong.core.common.log.Logger
import matchsong.core.common.result.OperationResult
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * [AudioRecorder] 的 Android 实现（M3.3-2，ARCHITECTURE.md §8.2，ADR-002）。
 *
 * 职责：
 * - 封装 [android.media.AudioRecord]：VOICE_RECOGNITION 源 / 16bit / mono PCM（ADR-002，R-2）；
 * - 采样率运行时降级：优先配置值，失败沿 44100 → 48000 → 16000 链探测（[SampleRateFallback]，M3.3-3，R-1）；
 * - 专用采集线程（单线程调度器）阻塞 read() → ShortArray → 归一化 FloatArray（-1.0..1.0），
 *   经带背压的 [Channel]（容量 4，SUSPEND 语义）产出 [AudioChunk] 帧流（§8.2/§14）；
 * - 采集异常统一经 [RecordingErrorMapper] 映射为类型化 [AppError]（M3.3-3，P9：禁止空 catch）。
 *
 * **不写入文件**：PCM 落盘是 M3.5-1（WavFileWriter/RecordingFileManager）的职责，
 * 本类只产出帧流；调用方负责将 [frames] 交给下游分析/落盘。
 *
 * 生命周期契约：
 * - [start]：未启动校验（已启动 → Failure(InitFailed)，与 FakeAudioRecorder 约定一致）；
 *   采样率探测/缓冲探测/构造/状态检查/启动任一失败 → Failure（InitFailed / MicBusy / PermissionRevoked / UnknownError）；
 * - [stop]：幂等；解除 read() 阻塞 → 关闭通道 → 等待采集线程退出 → 释放 AudioRecord；
 * - [frames]：冷流——collect 结束或协程取消即停止本次录音（资源释放经 NonCancellable 保证，§14.3）；
 * - 达到 [RecordingConfig.maxDurationMs] 自动正常停止（≤0 表示不限时），非错误路径。
 *
 * 采集期错误（read 错误码/异常）经映射后记录日志并结束帧流（通道关闭）；
 * 面向用户的错误呈现由上层（RecordingPort/状态机）负责——冻结接口仅暴露帧流，
 * 本类不持有额外错误通道。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAudioRecorder(
    private val sampleRateFallback: SampleRateFallback = SampleRateFallback(),
    private val logger: Logger = AndroidLogLogger(),
) : AudioRecorder {
    private val stateLock = Any()
    private var activeSession: ActiveSession? = null

    override fun start(config: RecordingConfig): OperationResult<Unit> {
        return synchronized(stateLock) {
            when {
                activeSession != null -> OperationResult.Failure(alreadyStartedError())
                else -> {
                    when (val created = createSession(config)) {
                        is OperationResult.Success -> {
                            activeSession = created.data
                            try {
                                launchCapture(created.data)
                                OperationResult.Success(Unit)
                            } catch (e: RuntimeException) {
                                // 采集线程创建失败（极低概率）：清理会话并映射错误
                                logger.e(LogTags.AUDIO, "启动采集线程失败", e)
                                stopInternal(created.data, waitForCompletion = false)
                                OperationResult.Failure(RecordingErrorMapper.map(e, AudioCaptureStage.INIT))
                            }
                        }
                        is OperationResult.Failure -> created
                    }
                }
            }
        }
    }

    override fun stop() {
        val session = synchronized(stateLock) { activeSession ?: return }
        stopInternal(session, waitForCompletion = true)
    }

    /**
     * 帧流（冷流）：收集当前会话的帧通道；collect 结束或协程取消即停止本次录音。
     */
    override val frames: Flow<AudioChunk> =
        flow {
            val session = synchronized(stateLock) { activeSession }
            if (session == null) {
                return@flow // 未启动：无帧可产出
            }
            try {
                emitAll(session.channel.receiveAsFlow())
            } finally {
                // 冷流契约：collect 结束/取消 → 停止本次录音（幂等；清理经 NonCancellable 保证）
                withContext(NonCancellable) {
                    stopInternal(session, waitForCompletion = false)
                }
            }
        }

    // ---- 启动流程 ----

    private fun createSession(config: RecordingConfig): OperationResult<ActiveSession> {
        // 1. 采样率运行时降级（M3.3-3）：优先配置值，失败沿降级链探测
        val sampleRate = sampleRateFallback.findWorkingSampleRate(config.sampleRateHz)
        if (sampleRate == null) {
            return OperationResult.Failure(
                RecordingErrorMapper.initFailed(
                    cause = IllegalStateException("所有候选采样率均不可用"),
                    details =
                        mapOf(
                            "preferredRate" to config.sampleRateHz.toString(),
                            "attemptedRates" to SampleRateFallback.DEFAULT_FALLBACK_CHAIN.joinToString(","),
                        ),
                ),
            )
        }
        // 2. 缓冲探测（ADR-002：getMinBufferSize 探测缓冲）
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_IN_MONO, ENCODING_PCM_16BIT)
        if (minBufferSize <= 0) {
            return OperationResult.Failure(
                RecordingErrorMapper.initFailed(
                    cause = IllegalStateException("AudioRecord.getMinBufferSize($sampleRate) 返回 $minBufferSize"),
                ),
            )
        }
        // 3. 构造 AudioRecord（SecurityException → 权限问题；参数非法 → InitFailed；其余 → UnknownError）
        val record =
            try {
                AudioRecord(
                    AUDIO_SOURCE,
                    sampleRate,
                    CHANNEL_IN_MONO,
                    ENCODING_PCM_16BIT,
                    minBufferSize * 2,
                )
            } catch (e: SecurityException) {
                logger.w(LogTags.AUDIO, "构造 AudioRecord 失败（权限缺失或已撤销）", e)
                return OperationResult.Failure(RecordingErrorMapper.map(e, AudioCaptureStage.INIT))
            } catch (e: IllegalArgumentException) {
                logger.w(LogTags.AUDIO, "构造 AudioRecord 失败（参数非法）", e)
                return OperationResult.Failure(RecordingErrorMapper.map(e, AudioCaptureStage.INIT))
            } catch (e: RuntimeException) {
                logger.e(LogTags.AUDIO, "构造 AudioRecord 失败（未预期异常）", e)
                return OperationResult.Failure(RecordingErrorMapper.map(e, AudioCaptureStage.INIT))
            }
        // 4. 状态检查：未初始化 → InitFailed
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            releaseQuietly(record, "未初始化状态释放")
            return OperationResult.Failure(
                RecordingErrorMapper.initFailed(
                    cause = IllegalStateException("AudioRecord.state=${record.state}，未进入 STATE_INITIALIZED"),
                ),
            )
        }
        // 5. 启动录音（被占用 → MicBusy；权限撤销 → PermissionRevoked）
        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            releaseQuietly(record, "启动失败释放")
            logger.w(LogTags.AUDIO, "startRecording 失败（麦克风可能被占用）", e)
            return OperationResult.Failure(RecordingErrorMapper.map(e, AudioCaptureStage.START))
        } catch (e: SecurityException) {
            releaseQuietly(record, "启动失败释放")
            logger.w(LogTags.AUDIO, "startRecording 失败（权限撤销）", e)
            return OperationResult.Failure(RecordingErrorMapper.map(e, AudioCaptureStage.START))
        } catch (e: RuntimeException) {
            releaseQuietly(record, "启动失败释放")
            logger.e(LogTags.AUDIO, "startRecording 失败（未预期异常）", e)
            return OperationResult.Failure(RecordingErrorMapper.map(e, AudioCaptureStage.START))
        }
        return OperationResult.Success(
            ActiveSession(
                record = record,
                minBufferSize = minBufferSize,
                config = config,
            ),
        )
    }

    private fun launchCapture(session: ActiveSession) {
        // 专用采集线程（§14.2：read() 阻塞于专用线程，独立于 UI 线程）
        val dispatcher =
            Executors.newSingleThreadExecutor { r ->
                Thread(r, CAPTURE_THREAD_NAME).apply { isDaemon = true }
            }.asCoroutineDispatcher()
        session.dispatcher = dispatcher
        session.job = CoroutineScope(SupervisorJob() + dispatcher).launch { captureLoop(session) }
    }

    // ---- 采集循环 ----

    private suspend fun captureLoop(session: ActiveSession) {
        val record = session.record
        val buffer = ShortArray(session.minBufferSize / 2) // 16bit mono：每样本 2 字节
        try {
            while (session.running.get() && !isDurationExceeded(session)) {
                val n = record.read(buffer, 0, buffer.size)
                when {
                    n < 0 -> {
                        // read 返回错误码（ERROR_INVALID_OPERATION 等）
                        if (session.running.get()) {
                            // 正常停止路径中 read 会被 stop() 打断并返回错误码，此时静默退出
                            handleReadFailure(session, IllegalStateException("AudioRecord.read 返回错误码 $n"))
                        }
                        return
                    }
                    n > 0 -> {
                        // 背压：通道满则挂起采集（容量 4，SUSPEND）
                        session.channel.send(toChunk(buffer, n))
                    }
                    else -> yield() // n == 0：无样本短读，让出调度避免空转
                }
            }
        } catch (e: CancellationException) {
            throw e // 结构化并发：取消必须继续传播，资源释放交 finally
        } catch (e: ClosedSendChannelException) {
            logger.d(LogTags.AUDIO, "采集通道已关闭，采集循环正常退出")
        } catch (e: SecurityException) {
            handleReadFailure(session, e)
        } catch (e: IllegalStateException) {
            handleReadFailure(session, e)
        } catch (e: Exception) {
            handleReadFailure(session, e)
        } finally {
            withContext(NonCancellable) {
                // 无论正常结束/错误/取消，都确保清理会话并释放资源
                synchronized(stateLock) {
                    if (activeSession === session) activeSession = null
                }
                releaseQuietly(record, "采集循环 finally 释放")
                session.channel.close()
                session.dispatcher?.close()
            }
        }
    }

    private fun handleReadFailure(
        session: ActiveSession,
        throwable: Throwable,
    ) {
        val error = RecordingErrorMapper.map(throwable, AudioCaptureStage.READ)
        logger.e(LogTags.AUDIO, "采集读取失败：${error.messageKey}", throwable)
        session.running.set(false)
    }

    private fun isDurationExceeded(session: ActiveSession): Boolean {
        val maxDurationMs = session.config.maxDurationMs
        return maxDurationMs > 0 && System.nanoTime() - session.startedNanos >= maxDurationMs * 1_000_000L
    }

    private fun toChunk(
        buffer: ShortArray,
        count: Int,
    ): AudioChunk {
        val samples = FloatArray(count)
        var sumSquares = 0.0
        var peak = 0.0
        for (i in 0 until count) {
            val value = buffer[i] / NORMALIZATION_FACTOR // Short.MIN_VALUE → -1.0，归一化 -1.0..1.0
            samples[i] = value
            val d = value.toDouble()
            sumSquares += d * d
            val abs = if (d < 0) -d else d
            if (abs > peak) peak = abs
        }
        return AudioChunk(
            samples = samples,
            rms = if (count == 0) 0.0 else sqrt(sumSquares / count),
            peak = peak,
        )
    }

    // ---- 停止/清理 ----

    private fun stopInternal(
        session: ActiveSession,
        waitForCompletion: Boolean,
    ) {
        val isCurrent =
            synchronized(stateLock) {
                if (activeSession === session) {
                    activeSession = null
                    true
                } else {
                    false
                }
            }
        if (!isCurrent) return // 已被采集循环自身清理，或已是更新的会话
        session.running.set(false)
        // 从采集线程外调用 stop() 解除 read() 阻塞（Android docs 要求 stop 与 read 不同线程）
        try {
            session.record.stop()
        } catch (e: IllegalStateException) {
            logger.d(LogTags.AUDIO, "AudioRecord 已停止或已释放", e)
        }
        session.channel.close() // 解除采集循环中挂起的 send()
        session.job?.cancel()
        if (waitForCompletion) {
            val joined = runBlocking { withTimeoutOrNull(STOP_JOIN_TIMEOUT_MS) { session.job?.join() } }
            if (joined == null) {
                logger.w(LogTags.AUDIO, "采集线程在 ${STOP_JOIN_TIMEOUT_MS}ms 内未退出")
            }
        }
        session.dispatcher?.close()
    }

    private fun releaseQuietly(
        record: AudioRecord,
        context: String,
    ) {
        try {
            record.release()
        } catch (e: Exception) {
            // P9：清理路径异常同样记录，不抛出
            logger.w(LogTags.AUDIO, "释放 AudioRecord 失败（$context）", e)
        }
    }

    private fun alreadyStartedError(): AppError.RecordingError.InitFailed =
        AppError.RecordingError.InitFailed(
            cause = IllegalStateException("AndroidAudioRecorder 已处于启动状态，需先 stop()"),
        )

    // ---- 会话状态 ----

    private class ActiveSession(
        val record: AudioRecord,
        val minBufferSize: Int,
        val config: RecordingConfig,
    ) {
        val running = AtomicBoolean(true)
        val channel = Channel<AudioChunk>(CHANNEL_CAPACITY)
        val startedNanos: Long = System.nanoTime()

        @Volatile
        var job: Job? = null

        @Volatile
        var dispatcher: ExecutorCoroutineDispatcher? = null
    }

    private companion object {
        const val CHANNEL_CAPACITY: Int = 4
        const val CAPTURE_THREAD_NAME: String = "MatchSong-AudioCapture"
        const val STOP_JOIN_TIMEOUT_MS: Long = 2_000L
        const val NORMALIZATION_FACTOR: Float = 32768f

        val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION
        val CHANNEL_IN_MONO: Int = AudioFormat.CHANNEL_IN_MONO
        val ENCODING_PCM_16BIT: Int = AudioFormat.ENCODING_PCM_16BIT
    }
}
