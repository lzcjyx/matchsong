package matchsong.core.audio.algorithm

import matchsong.core.audio.api.AudioChunk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * M3.6-1 VolumeMeter 测试：音量级三标志判定（FR-REC-4，阈值 data-model §5.1 Q-1~Q-3）。
 */
class VolumeMeterTest {
    private val meter = VolumeMeter()

    /** 生成采样率 44100Hz、时长 1s、频率 440Hz 的正弦波 chunk（整数周期 → rms 精确为 amplitude/√2）。 */
    private fun sineChunk(
        amplitude: Double = 1.0,
        sampleRate: Int = 44100,
    ): AudioChunk {
        val samples =
            FloatArray(sampleRate) { i ->
                (amplitude * sin(2 * PI * 440.0 * i / sampleRate)).toFloat()
            }
        val rms = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)
        val peak = samples.maxOf { abs(it.toDouble()) }
        return AudioChunk(samples, rms, peak)
    }

    @Test
    fun `正弦波 chunk 生成正确的音量级`() {
        val chunk = sineChunk()
        val level = meter.computeVolume(chunk)

        assertEquals(chunk.rms, level.rms)
        assertEquals(1.0 / sqrt(2.0), chunk.rms, 1e-3) // 幅度 1.0 正弦波 rms ≈ 0.707
        assertFalse(level.isTooQuiet) // 0.707 ≥ Q-2 0.02
        assertFalse(level.isClipping) // 正弦波无连续满幅样本
        assertTrue(level.hasInput) // 0.707 > Q-1 0.01
    }

    @Test
    fun `低音量 chunk 标记 isTooQuiet 且仍有输入`() {
        val chunk = sineChunk(amplitude = 0.02) // rms ≈ 0.0141 ∈ (Q-1 0.01, Q-2 0.02)
        val level = meter.computeVolume(chunk)

        assertTrue(level.isTooQuiet)
        assertTrue(level.hasInput)
        assertFalse(level.isClipping)
    }

    @Test
    fun `静音 chunk 无输入`() {
        val chunk = AudioChunk(FloatArray(1024), rms = 0.0, peak = 0.0)
        val level = meter.computeVolume(chunk)

        assertFalse(level.hasInput)
        assertTrue(level.isTooQuiet)
        assertFalse(level.isClipping)
    }

    @Test
    fun `连续满幅样本达到 Q-3 判定削波`() {
        val samples = FloatArray(64) { 0.5f }
        samples[10] = 1.0f
        samples[11] = 1.0f
        samples[12] = 1.0f
        val level = meter.computeVolume(AudioChunk(samples, rms = 0.5, peak = 1.0))

        assertTrue(level.isClipping)
    }

    @Test
    fun `不足 Q-3 连续满幅样本数不判定削波`() {
        val samples = FloatArray(64) { 0.5f }
        samples[10] = 1.0f
        samples[11] = 1.0f
        val level = meter.computeVolume(AudioChunk(samples, rms = 0.5, peak = 1.0))

        assertFalse(level.isClipping)
    }

    @Test
    fun `rms 恰为阈值时按严格不等号判定`() {
        // Q-2：rms == 0.02 未低于阈值 → 不算音量过低；低于才判定
        assertFalse(meter.computeVolume(AudioChunk(FloatArray(1), 0.02, 0.02)).isTooQuiet)
        assertTrue(meter.computeVolume(AudioChunk(FloatArray(1), 0.019999, 0.02)).isTooQuiet)
        // Q-1：rms == 0.01 不大于静音阈值 → 无输入；大于才有输入
        assertFalse(meter.computeVolume(AudioChunk(FloatArray(1), 0.01, 0.01)).hasInput)
        assertTrue(meter.computeVolume(AudioChunk(FloatArray(1), 0.010001, 0.01)).hasInput)
    }

    @Test
    fun `自定义阈值集中配置可覆盖默认判定`() {
        val strict = VolumeMeter(QualityThresholds(quietRmsThreshold = 0.5))
        val chunk = AudioChunk(FloatArray(1), rms = 0.4, peak = 0.4)

        assertFalse(meter.computeVolume(chunk).isTooQuiet)
        assertTrue(strict.computeVolume(chunk).isTooQuiet)
    }

    @Test
    fun `QualityThresholds 校验非法配置`() {
        assertThrows(IllegalArgumentException::class.java) { QualityThresholds(quietRmsThreshold = 0.01) } // 须 > Q-1
        assertThrows(
            IllegalArgumentException::class.java,
        ) { QualityThresholds(clippingConsecutiveFullScaleSamples = 0) }
        assertThrows(IllegalArgumentException::class.java) { QualityThresholds(clippingFullScaleMagnitude = 1.5f) }
    }
}
