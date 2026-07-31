package matchsong.core.audio.algorithm

import matchsong.core.audio.api.WavFileSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * M4.6-2 质量检测测试套件：PLAN M4.6 场景（TESTING.md §5.3）。
 *
 * 夹具用 core:audio 自身的 WavFileWriter 生成（避免 core:testing 循环依赖），
 * 信号参数与 test-fixture-manifest 合成夹具一致。断言 isUsable 与原因精确匹配（ACC-7/8）。
 */
class QualityAnalyzerTest {
    @TempDir
    lateinit var tempDir: File

    private val analyzer = QualityAnalyzer()

    private enum class Signal { SINE, SILENCE, NOISE, CLIPPED }

    private fun generateWav(
        name: String,
        signal: Signal,
        durationSec: Double,
        amplitude: Double,
    ): File {
        val n = (44_100 * durationSec).toInt()
        val pcm = File(tempDir, "$name.pcm")
        val out = java.io.DataOutputStream(pcm.outputStream())
        val rnd = java.util.Random(42)
        for (i in 0 until n) {
            val t = i.toDouble() / 44_100.0
            val v: Double =
                when (signal) {
                    Signal.SINE -> amplitude * sin(2.0 * PI * 440.0 * t)
                    Signal.SILENCE -> 0.0
                    Signal.NOISE -> (rnd.nextDouble() * 2.0 - 1.0) * amplitude
                    // 削波：原始信号超过满幅被硬切（触顶 ±1.0，产生满幅样本段）
                    Signal.CLIPPED -> (amplitude * sin(2.0 * PI * 440.0 * t)).coerceIn(-1.0, 1.0)
                }
            val sample = (v * 32767).toInt().coerceIn(-32768, 32767)
            // WAV PCM16 为 little-endian：低字节在前
            out.writeByte(sample and 0xFF)
            out.writeByte((sample shr 8) and 0xFF)
        }
        out.close()
        val wav = File(tempDir, "$name.wav")
        WavFileWriter.writePcmToWav(pcm, wav, 44_100, 1, 16)
        return wav
    }

    private fun analyze(
        signal: Signal,
        durationSec: Double,
        amplitude: Double = 0.5,
    ): AudioQualityReport {
        val wav = generateWav("fixture-${signal.name}-$durationSec", signal, durationSec, amplitude)
        return analyzer.analyze(WavFileSource(wav))
    }

    @Test
    fun `normal sine is usable`() {
        val report = analyze(Signal.SINE, 15.0, amplitude = 0.5)
        assertTrue(report.isUsable, "正常音量正弦应可用，warnings=${report.warnings}")
        assertEquals(QualityAction.ANALYZE, report.recommendedAction)
        assertTrue(report.confidence >= 0.5)
    }

    @Test
    fun `silence is rejected as SILENT`() {
        val report = analyze(Signal.SILENCE, 15.0)
        assertFalse(report.isUsable)
        assertTrue(report.warnings.contains(QualityWarning.SILENT), "静音应判 SILENT，实际=${report.warnings}")
        assertEquals(QualityAction.RETRY, report.recommendedAction)
    }

    @Test
    fun `clipped signal is rejected as CLIPPING`() {
        // amplitude 1.5 的正弦硬削到 ±1.0 → 触顶产生满幅样本段（Q-3 削波）
        val report = analyze(Signal.CLIPPED, 15.0, amplitude = 1.5)
        assertFalse(report.isUsable)
        assertTrue(report.warnings.contains(QualityWarning.CLIPPING), "削波应判 CLIPPING，实际=${report.warnings}")
        assertTrue(report.clippingRatio > 0.0)
    }

    @Test
    fun `too short recording is rejected as TOO_SHORT`() {
        val report = analyze(Signal.SINE, 3.0, amplitude = 0.5)
        assertFalse(report.isUsable)
        assertTrue(report.warnings.contains(QualityWarning.TOO_SHORT), "3s 录音应判过短，实际=${report.warnings}")
        assertTrue(report.durationMs < 10_000)
    }

    @Test
    fun `quiet signal is rejected`() {
        val report = analyze(Signal.SINE, 15.0, amplitude = 0.005)
        assertFalse(report.isUsable, "极低幅值应不可用，实际=${report.warnings}")
        assertTrue(report.quietRatio > 0.8)
    }

    @Test
    fun `white noise is rejected`() {
        val report = analyze(Signal.NOISE, 15.0, amplitude = 0.5)
        // 白噪声：非静音但无稳定有效声音 → 期望被拒（INSUFFICIENT_VOICE 或 NOISY）
        assertFalse(report.isUsable, "白噪声不应可用，实际=${report.warnings}")
    }

    @Test
    fun `short active voice within long recording rejected as insufficient`() {
        // 15s 正弦中仅前 2s 保留（模拟"有效演唱片段不足"）
        val wav = generateWav("partial", Signal.SINE, 15.0, 0.5)
        val samples = WavFileReader().read(wav).normalizedSamples()
        val keep = 2.0 / 15.0
        val truncated = samples.copyOfRange(0, (samples.size * keep).toInt())
        val frames = AudioFramePipeline.process(truncated)
        val report =
            analyzer.analyze(
                object : matchsong.core.audio.api.AudioFrameSource {
                    override fun readFrames(): List<Frame> = frames
                },
            )
        assertFalse(report.isUsable, "有效片段不足应不可用，实际=${report.warnings}")
    }
}
