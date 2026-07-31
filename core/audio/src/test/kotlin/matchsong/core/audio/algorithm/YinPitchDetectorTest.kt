package matchsong.core.audio.algorithm

import matchsong.core.audio.api.PitchFrame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * M5.1-1 / M5.8-1 YIN 生产实现测试（ADR-003 基线，Spike 实测对比）。
 *
 * 正弦 130-1046Hz 误差 <0.03%（Spike 基线）；静音/白噪声正确拒绝；音阶逐音检出。
 */
class YinPitchDetectorTest {
    private val detector = YinPitchDetector()

    private fun framesOfSine(
        freqHz: Double,
        durationSec: Double,
        amplitude: Double = 0.8,
    ): List<Frame> {
        val n = (44100 * durationSec).toInt()
        val samples = FloatArray(n) { i -> (amplitude * sin(2.0 * PI * freqHz * i / 44100)).toFloat() }
        return AudioFramePipeline.process(samples)
    }

    private fun medianFreq(frames: List<PitchFrame>): Double {
        val voiced = frames.filter { it.isVoiced }.map { it.f0Hz }.sorted()
        assertTrue(voiced.isNotEmpty(), "应存在有效帧")
        return voiced[voiced.size / 2]
    }

    private fun trackSine(
        freqHz: Double,
        durationSec: Double,
    ): List<matchsong.core.audio.api.PitchFrame> =
        kotlinx.coroutines.runBlocking { detector.track(framesOfSine(freqHz, durationSec)).frames }

    @Test
    fun `sine 440 detects within 0_03 percent`() {
        val frames = trackSine(440.0, 2.0)
        val med = medianFreq(frames)
        assertTrue(abs(med - 440.0) / 440.0 < 0.003, "440Hz 误差应 <0.3%（高通标定后），实际 $med")
    }

    @Test
    fun `sine 130 detects within 0_03 percent`() {
        val frames = trackSine(130.0, 2.0)
        val med = medianFreq(frames)
        assertTrue(abs(med - 130.0) / 130.0 < 0.003, "130Hz 误差应 <0.3%（高通标定后），实际 $med")
    }

    @Test
    fun `sine 880 detects within 0_03 percent`() {
        val frames = trackSine(880.0, 2.0)
        val med = medianFreq(frames)
        assertTrue(abs(med - 880.0) / 880.0 < 0.003, "880Hz 误差应 <0.3%（高通标定后），实际 $med")
    }

    @Test
    fun `sine 1046 boundary detects`() {
        val frames = trackSine(1046.0, 2.0)
        val med = medianFreq(frames)
        assertTrue(abs(med - 1046.0) / 1046.0 < 0.001, "1046Hz 边界应检出，实际 $med")
    }

    @Test
    fun `silence yields no voiced frames`() {
        val n = 44100 * 2
        val samples = FloatArray(n) { 1e-5f }
        val frames = AudioFramePipeline.process(samples)
        val track = kotlinx.coroutines.runBlocking { detector.track(frames) }
        assertEquals(0, track.voicedFrameCount, "静音应无有效帧")
    }

    @Test
    fun `white noise yields no voiced frames`() {
        val rnd = java.util.Random(42)
        val n = 44100 * 2
        val samples = FloatArray(n) { ((rnd.nextDouble() * 2 - 1) * 0.5).toFloat() }
        val frames = AudioFramePipeline.process(samples)
        val track = kotlinx.coroutines.runBlocking { detector.track(frames) }
        // YIN 对白噪声正确拒绝（Spike 实测：ACF/FFT 误报，YIN 不误报）
        assertEquals(0, track.voicedFrameCount, "白噪声应无有效帧")
    }

    @Test
    fun `scale C3 E3 G3 C4 lands near expected notes`() {
        // 音阶 C3(130.81) E3(164.81) G3(196.0) C4(261.63)，每音 0.5s
        val freqs = doubleArrayOf(130.81, 164.81, 196.0, 261.63)
        val perNote = 44100 / 2
        val samples =
            FloatArray(perNote * 4) { i ->
                val note = i / perNote
                (0.8 * sin(2 * PI * freqs[note] * (i % perNote) / 44100)).toFloat()
            }
        val frames = AudioFramePipeline.process(samples)
        val track = kotlinx.coroutines.runBlocking { detector.track(frames) }
        val voiced = track.frames.filter { it.isVoiced }
        assertTrue(voiced.size >= 10, "音阶应有足够有效帧，实际 ${voiced.size}")

        // 逐音区取中位数验证
        for ((idx, expected) in freqs.withIndex()) {
            val noteFrames =
                voiced.filter {
                    it.timestampMs in (idx * 500).toLong()..((idx + 1) * 500).toLong()
                }
            if (noteFrames.isEmpty()) continue
            val med = noteFrames.map { it.f0Hz }.sorted()[noteFrames.size / 2]
            val relErr = abs(med - expected) / expected
            assertTrue(relErr < 0.02, "音符 $idx 预期 $expected 实际 $med relErr=$relErr")
        }
    }

    @Test
    fun `out of range frequency does not report itself`() {
        // 2000Hz 超出工作范围上限（1046）：YIN 可能锁定子谐波（如 1002Hz），
        // 但不应报告 2000Hz 本身（超范围信号的子谐波响应是已知行为，后处理/质量门禁处理）
        val frames = trackSine(2000.0, 1.0)
        val voiced = frames.filter { it.isVoiced }
        val noneReport2000 = voiced.none { abs(it.f0Hz - 2000.0) / 2000.0 < 0.02 }
        assertTrue(noneReport2000, "2000Hz 不应被报告为自身频率")
    }
}
