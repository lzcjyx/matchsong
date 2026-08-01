package matchsong.core.audio.analysis

import kotlinx.coroutines.runBlocking
import matchsong.core.audio.algorithm.WavFileWriter
import matchsong.core.audio.api.WavFileSource
import matchsong.domain.analysis.AnalysisWarning
import matchsong.domain.analysis.ConfidenceLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * M5.6-1 AnalyzeRecordingUseCase 测试（P6 门禁 + 完整结果 + 置信度）。
 */
class AnalyzeRecordingUseCaseTest {
    @TempDir
    lateinit var tempDir: File

    private val useCase = AnalyzeRecordingUseCase()

    private fun wavSource(
        name: String,
        signal: (Double) -> Double,
        durationSec: Double,
    ): WavFileSource {
        val n = (44100 * durationSec).toInt()
        val pcm = File(tempDir, "$name.pcm")
        val out = java.io.DataOutputStream(pcm.outputStream())
        for (i in 0 until n) {
            val v = signal(i.toDouble() / 44100.0)
            val sample = (v * 32767).toInt().coerceIn(-32768, 32767)
            out.writeByte(sample and 0xFF)
            out.writeByte((sample shr 8) and 0xFF)
        }
        out.close()
        val wav = File(tempDir, "$name.wav")
        WavFileWriter.writePcmToWav(pcm, wav, 44100, 1, 16)
        return WavFileSource(wav)
    }

    private fun sine(
        freq: Double,
        durationSec: Double = 15.0,
    ): WavFileSource = wavSource("sine-$freq", { t -> 0.5 * sin(2 * PI * freq * t) }, durationSec)

    private fun silence(durationSec: Double = 15.0): WavFileSource = wavSource("silence", { 0.0 }, durationSec)

    @Test
    fun `quality failure short circuits`() {
        val result = runBlocking { useCase(silence()) }
        assertEquals(false, result.qualityUsable)
        assertNull(result.vocalRange)
        assertNull(result.comfortRange)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
    }

    @Test
    fun `valid singing-like input produces full result`() {
        val result = runBlocking { useCase(sine(220.0)) }
        assertEquals(true, result.qualityUsable)
        assertNotNull(result.vocalRange)
        assertNotNull(result.comfortRange)
        assertNotNull(result.stability)
        assertTrue(result.voicedFrameCount > 0)
        // 220Hz ≈ MIDI 57 (A3)；稳定音域应接近
        result.vocalRange?.let { range ->
            assertTrue(range.stableLowestMidi != null)
            assertTrue(range.sampleSufficiency)
            // A3=57 附近（P5/P95 窄区间）
            assertTrue(
                Math.abs(range.stableLowestMidi!! - 57.0) < 2.0 &&
                    Math.abs(range.stableHighestMidi!! - 57.0) < 2.0,
                "220Hz 音域应接近 A3(57)，实际 ${range.stableLowestMidi}-${range.stableHighestMidi}",
            )
        }
        assertEquals("1.0.0", result.algorithmVersion)
    }

    @Test
    fun `confidence level is high for clean signal`() {
        val result = runBlocking { useCase(sine(440.0)) }
        assertTrue(
            result.confidenceLevel == ConfidenceLevel.HIGH || result.confidenceLevel == ConfidenceLevel.MEDIUM,
            "干净信号置信度应 HIGH 或 MEDIUM，实际 ${result.confidenceLevel}",
        )
    }

    @Test
    fun `speech-dominant glide produces no range and LOW confidence`() {
        // BUG-015：连续说话式音高滑动（语调连续变化）→ 稳定片段比例过低 →
        // 判定非歌唱语音为主，拒绝生成正式音域/舒适区（按数据不足，FR-ANAL-8）
        val glide =
            wavSource(
                "glide",
                { t ->
                    // 相位积分：瞬时频率 300→600Hz 线性滑动（15s），模拟说话语调
                    0.5 * sin(2 * PI * (300.0 * t + 10.0 * t * t))
                },
                15.0,
            )
        val result = runBlocking { useCase(glide) }
        assertNull(result.vocalRange, "说话式滑动不应生成音域")
        assertNull(result.comfortRange, "说话式滑动不应生成舒适区")
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
        assertTrue(
            result.warnings.contains(AnalysisWarning.INSUFFICIENT_SAMPLES),
            "应提示有效演唱片段不足，实际 ${result.warnings}",
        )
    }
}
