package matchsong.pitch

import kotlin.math.abs
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * M-1.5 Spike 单元测试。
 *
 * 这些测试守护 Spike 的可观察行为契约，而非实现细节：
 * - YIN 对干净正弦波应给出高精度频率（< 1% 误差）
 * - 静音应被判为无音高（NaN）
 * - 白噪声不应产生稳定音高（detRate 低或 NaN）
 * - 削波信号基频应仍被检出（YIN）
 * - 音阶逐帧检出应落在对应音符附近
 * - FFT 对干净正弦精度高；ACF 已知弱点（八度错误）以八度容差断言
 */
class PitchSpikeTest {

    companion object {
        @JvmStatic
        fun sineCases(): List<Arguments> = listOf(
            Arguments.of(130.0, "C3 male low"),
            Arguments.of(220.0, "A3"),
            Arguments.of(440.0, "A4"),
            Arguments.of(880.0, "A5"),
            Arguments.of(1046.0, "C6 female high"),
        )
    }

    @ParameterizedTest(name = "YIN detects sine {1} ({0}Hz) within 1%")
    @MethodSource("sineCases")
    fun `YIN detects sine within 1 percent`(freqHz: Double, name: String) {
        val sig = Signals.sine(freqHz, 1.0)
        val frames = Pitch.detectFrames(sig, "YIN", frameSize = 2048, hopSize = 1024)
        val detected = frames.filter { !it.f0Hz.isNaN() && it.f0Hz in 20.0..2000.0 }
        assertTrue(detected.isNotEmpty(), "YIN should detect something for sine $name")
        val medianFreq = detected.map { it.f0Hz }.sorted()[detected.size / 2]
        val relErr = abs(medianFreq - freqHz) / freqHz
        assertTrue(relErr < 0.01, "YIN median $medianFreq vs $freqHz, relErr=$relErr should be < 1%")
    }

    @Test
    fun `silence yields no pitch`() {
        val sig = Signals.silence(1.0)
        val frames = Pitch.detectFrames(sig, "YIN", frameSize = 2048, hopSize = 1024)
        val allNoPitch = frames.all { it.f0Hz.isNaN() }
        assertTrue(allNoPitch, "silence should yield no pitch (all NaN)")
    }

    @Test
    fun `white noise has low detection rate or NaN`() {
        val sig = Signals.whiteNoise(1.0)
        val frames = Pitch.detectFrames(sig, "YIN", frameSize = 2048, hopSize = 1024)
        val detRate = frames.count { !it.f0Hz.isNaN() } / frames.size.toDouble()
        assertTrue(detRate < 0.5, "white noise detection rate $detRate should be low (<0.5)")
    }

    @Test
    fun `YIN detects fundamental of clipped signal`() {
        val sig = Signals.clipped(440.0, 1.0, clip = 0.3)
        val frames = Pitch.detectFrames(sig, "YIN", frameSize = 2048, hopSize = 1024)
        val detected = frames.filter { !it.f0Hz.isNaN() }
        assertTrue(detected.isNotEmpty(), "YIN should detect clipped signal fundamental")
        val medianFreq = detected.map { it.f0Hz }.sorted()[detected.size / 2]
        // 削波产生强奇次谐波，YIN 应仍锁定基频 440Hz（容差放宽到 5%）
        val relErr = abs(medianFreq - 440.0) / 440.0
        assertTrue(relErr < 0.05, "YIN clipped median $medianFreq vs 440, relErr=$relErr < 5%")
    }

    @Test
    fun `scale frames land near expected notes`() {
        val midiNotes = intArrayOf(48, 52, 55, 60) // C3 E3 G3 C4
        val sig = Signals.scale(midiNotes, durationSec = 0.5)
        val frames = Pitch.detectFrames(sig, "YIN", frameSize = 2048, hopSize = 1024)
        // 取每段中间一帧的检出，应接近对应音符频率
        val sr = Signals.SAMPLE_RATE
        val framesPerNote = (sr * 0.5).toInt() / 1024
        for ((idx, midi) in midiNotes.withIndex()) {
            val center = idx * framesPerNote + framesPerNote / 2
            if (center >= frames.size) continue
            val f = frames[center]
            if (f.f0Hz.isNaN()) continue
            val expected = Signals.midiToFreq(midi)
            val relErr = abs(f.f0Hz - expected) / expected
            assertTrue(relErr < 0.02, "note ${idx} midi=$midi f0=${f.f0Hz} expected=$expected relErr=$relErr < 2%")
        }
    }

    @Test
    fun `FFT detects clean sine within 3 percent`() {
        for (freq in listOf(220.0, 440.0)) {
            val sig = Signals.sine(freq, 1.0)
            val frames = Pitch.detectFrames(sig, "FFT", frameSize = 2048, hopSize = 1024)
            val detected = frames.filter { !it.f0Hz.isNaN() }
            assertTrue(detected.isNotEmpty(), "FFT should detect sine $freq")
            val med = detected.map { it.f0Hz }.sorted()[detected.size / 2]
            val relErr = abs(med - freq) / freq
            // FFT 频谱主峰对干净正弦精度高，容差 3%
            assertTrue(relErr < 0.03, "FFT median $med vs $freq relErr=$relErr < 3%")
        }
    }

    @Test
    fun `ACF detects harmonic-related frequency for clean sine`() {
        // ACF 已知弱点（教科书行为）：自相关在整数倍周期处都有局部峰，
        // 朴素 ACF 会锁定到子谐波/倍频（例如 220Hz 被检为 220/3 ≈ 73.3Hz）。
        // 这正是 YIN 引入累积均值归一化（CMND）要解决的问题。
        // Spike 记录该真实行为：断言检出频率与真值之比为小整数比（k 或 1/k，k<=4）。
        for (freq in listOf(220.0, 440.0)) {
            val sig = Signals.sine(freq, 1.0)
            val frames = Pitch.detectFrames(sig, "ACF", frameSize = 2048, hopSize = 1024)
            val detected = frames.filter { !it.f0Hz.isNaN() }
            assertTrue(detected.isNotEmpty(), "ACF should detect something for sine $freq")
            val med = detected.map { it.f0Hz }.sorted()[detected.size / 2]
            val harmonicRelated = (1..4).any { k ->
                abs(med - freq * k) / (freq * k) < 0.05 ||
                    abs(med - freq / k) / (freq / k) < 0.05
            }
            assertTrue(
                harmonicRelated,
                "ACF median $med vs $freq should be harmonic-related (k or 1/k, k<=4)",
            )
        }
    }
}
