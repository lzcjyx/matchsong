package matchsong.core.testing.fake

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import matchsong.core.audio.api.RecordingConfig
import matchsong.core.common.result.OperationResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

/**
 * FakeAudioRecorder 测试（M1.4-5 验收：输出帧流的 RMS/频率符合配置）。
 */
class FakeAudioRecorderTest {
    private val sampleRate = 44_100

    @Test
    fun `正弦信号的 RMS 与峰值符合理论值`() =
        runTest {
            val recorder =
                FakeAudioRecorder(
                    FakeAudioSignal(type = FakeSignalType.SINE, frequencyHz = 440.0, amplitude = 0.5),
                )
            assertEquals(
                OperationResult.Success(Unit),
                recorder.start(RecordingConfig(sampleRateHz = sampleRate, maxDurationMs = 500)),
            )
            val chunks = recorder.frames.take(2).toList()
            assertEquals(2, chunks.size)
            chunks.forEach { chunk ->
                assertEquals(0.5, chunk.peak, 1e-3, "峰值应等于幅值")
                assertEquals(0.5 / sqrt(2.0), chunk.rms, 5e-3, "正弦 RMS 应为 A/√2")
            }
        }

    @Test
    fun `正弦信号频率与配置一致`() =
        runTest {
            val frequency = 440.0
            val recorder =
                FakeAudioRecorder(
                    FakeAudioSignal(type = FakeSignalType.SINE, frequencyHz = frequency, amplitude = 0.5),
                )
            recorder.start(RecordingConfig(sampleRateHz = sampleRate, maxDurationMs = 500))
            val samples = recorder.frames.first().samples

            var crossings = 0
            for (i in 1 until samples.size) {
                val prev = samples[i - 1]
                val curr = samples[i]
                val falling = prev < 0f && curr >= 0f
                val rising = prev > 0f && curr <= 0f
                if (falling || rising) crossings++
            }
            val durationSec = samples.size.toDouble() / sampleRate
            val estimatedHz = crossings / 2.0 / durationSec
            assertEquals(frequency, estimatedHz, frequency * 0.02, "过零率估计频率应接近配置值")
        }

    @Test
    fun `静音信号 RMS 与峰值均为零`() =
        runTest {
            val recorder = FakeAudioRecorder(FakeAudioSignal(type = FakeSignalType.SILENCE))
            recorder.start(RecordingConfig(sampleRateHz = sampleRate, maxDurationMs = 200))
            val chunk = recorder.frames.first()
            assertEquals(0.0, chunk.rms)
            assertEquals(0.0, chunk.peak)
            assertTrue(chunk.samples.all { it == 0f })
        }

    @Test
    fun `削波信号样本被限幅在 clipLevel`() =
        runTest {
            val recorder =
                FakeAudioRecorder(
                    FakeAudioSignal(
                        type = FakeSignalType.CLIPPED,
                        frequencyHz = 440.0,
                        amplitude = 0.5,
                        clipLevel = 0.3,
                    ),
                )
            recorder.start(RecordingConfig(sampleRateHz = sampleRate, maxDurationMs = 200))
            val chunk = recorder.frames.first()
            assertTrue(chunk.samples.all { it >= -0.3f && it <= 0.3f })
            assertEquals(0.3, chunk.peak, 1e-6, "削波后峰值应等于限幅")
        }

    @Test
    fun `噪声信号 RMS 接近幅值除以根号三`() =
        runTest {
            val amplitude = 0.5
            val recorder =
                FakeAudioRecorder(
                    FakeAudioSignal(type = FakeSignalType.NOISE, amplitude = amplitude),
                )
            recorder.start(RecordingConfig(sampleRateHz = sampleRate, maxDurationMs = 500))
            val chunks = recorder.frames.take(3).toList()
            chunks.forEach { chunk ->
                assertEquals(amplitude / sqrt(3.0), chunk.rms, 2e-2, "均匀白噪声 RMS 应为 A/√3")
                assertTrue(chunk.peak in 0.4..0.5)
            }
        }

    @Test
    fun `重复 start 返回 Failure 且 stop 后可重启`() =
        runTest {
            val recorder = FakeAudioRecorder()
            assertEquals(OperationResult.Success(Unit), recorder.start(RecordingConfig()))
            val second = recorder.start(RecordingConfig())
            assertInstanceOf(OperationResult.Failure::class.java, second)
            recorder.stop()
            assertEquals(OperationResult.Success(Unit), recorder.start(RecordingConfig()))
        }

    @Test
    fun `未 start 即收集帧流抛出异常`() =
        runTest {
            val recorder = FakeAudioRecorder()
            val exception =
                assertThrows(IllegalStateException::class.java) {
                    kotlinx.coroutines.runBlocking { recorder.frames.first() }
                }
            assertEquals(true, exception.message?.contains("start") == true)
        }
}
