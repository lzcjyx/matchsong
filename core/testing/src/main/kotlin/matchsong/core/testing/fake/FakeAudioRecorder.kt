package matchsong.core.testing.fake

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import matchsong.core.audio.api.AudioChunk
import matchsong.core.audio.api.AudioRecorder
import matchsong.core.audio.api.RecordingConfig
import matchsong.core.common.error.AppError
import matchsong.core.common.result.OperationResult
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** 假流信号类型（test-fixture-manifest.md §2.4 / ARCHITECTURE.md §8.2，M1.4-5）。 */
enum class FakeSignalType { SINE, SILENCE, NOISE, CLIPPED }

/** FakeAudioRecorder 的信号配置。 */
data class FakeAudioSignal(
    val type: FakeSignalType = FakeSignalType.SINE,
    val frequencyHz: Double = 440.0,
    val amplitude: Double = 0.5,
    val clipLevel: Double = 0.3,
    val seed: Long = 42L,
)

/**
 * 程序化假录音机（ARCHITECTURE.md §8.2/§16.1，FR-QUAL-4 的 Fake Frame Source，M1.4-5）。
 *
 * 按 [FakeAudioSignal] 生成正弦/静音/噪声/削波帧流，JVM 可测，仅 debug/test 引入（FR-SHELL-3）。
 * [frames] 为**冷流**：collect 时按 start() 传入的 [RecordingConfig]（采样率/最大时长）
 * 逐 chunk 生成，每个 chunk 附带 RMS/峰值聚合指标；collect 结束或协程取消即停止。
 * [stop] 重置状态以允许再次 start（幂等）。
 */
class FakeAudioRecorder(
    private val signal: FakeAudioSignal = FakeAudioSignal(),
) : AudioRecorder {
    /** 每个 chunk 的时长（毫秒），模拟采集帧周期。 */
    val chunkDurationMs: Long = 100L

    @Volatile
    private var startConfig: RecordingConfig? = null

    override val frames: Flow<AudioChunk> =
        flow {
            val config =
                startConfig ?: error(
                    "FakeAudioRecorder.frames 需先调用 start(config) 才能收集",
                )
            val sampleRate = config.sampleRateHz
            val chunkSize = (sampleRate * chunkDurationMs / 1000L).toInt().coerceAtLeast(1)
            val totalSamples = sampleRate.toLong() * config.maxDurationMs / 1000L
            var startSample = 0L
            while (startSample < totalSamples && currentCoroutineContext().isActive) {
                val count = min(chunkSize, (totalSamples - startSample).toInt())
                emit(buildChunk(sampleRate, startSample, count))
                startSample += count
            }
        }

    override fun start(config: RecordingConfig): OperationResult<Unit> {
        if (startConfig != null) {
            return OperationResult.Failure(
                AppError.RecordingError.InitFailed(
                    cause = IllegalStateException("FakeAudioRecorder 已处于启动状态，需先 stop()"),
                ),
            )
        }
        startConfig = config
        return OperationResult.Success(Unit)
    }

    override fun stop() {
        startConfig = null
    }

    private fun buildChunk(
        sampleRate: Int,
        startSample: Long,
        count: Int,
    ): AudioChunk {
        val samples = FloatArray(count)
        val random = Random(signal.seed + startSample)
        for (i in 0 until count) {
            val t = (startSample + i).toDouble() / sampleRate
            val value =
                when (signal.type) {
                    FakeSignalType.SINE -> signal.amplitude * sin(2.0 * PI * signal.frequencyHz * t)
                    FakeSignalType.SILENCE -> 0.0
                    FakeSignalType.NOISE -> (random.nextDouble() * 2.0 - 1.0) * signal.amplitude
                    FakeSignalType.CLIPPED -> {
                        val raw = signal.amplitude * sin(2.0 * PI * signal.frequencyHz * t)
                        raw.coerceIn(-signal.clipLevel, signal.clipLevel)
                    }
                }
            samples[i] = value.toFloat()
        }
        var sumSquares = 0.0
        var peak = 0.0
        for (s in samples) {
            sumSquares += s * s
            val absValue = abs(s.toDouble())
            if (absValue > peak) peak = absValue
        }
        val rms = sqrt(sumSquares / samples.size)
        return AudioChunk(samples = samples, rms = rms, peak = peak)
    }
}
