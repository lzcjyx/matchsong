package matchsong.core.audio.analysis

import kotlinx.coroutines.runBlocking
import matchsong.core.audio.algorithm.WavFileWriter
import matchsong.core.audio.api.WavFileSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * M5.8-1 分析一致性测试（FR-RECM-7 前置，ACC-13 分析侧）。
 *
 * 同一输入跑两次 → 稳定音域/舒适音区完全一致（确定性算法）。
 */
class AnalysisConsistencyTest {
    @TempDir
    lateinit var tempDir: File

    private val useCase = AnalyzeRecordingUseCase()

    private fun wavSource(
        name: String,
        durationSec: Double = 15.0,
    ): WavFileSource {
        val n = (44100 * durationSec).toInt()
        val pcm = File(tempDir, "$name.pcm")
        val out = java.io.DataOutputStream(pcm.outputStream())
        // 音阶 C3-E3-G3-C4 循环（稳定、确定性）
        val freqs = doubleArrayOf(130.81, 164.81, 196.0, 261.63)
        for (i in 0 until n) {
            val noteIdx = (i / (44100 / 2)).toInt() % 4
            val v = 0.5 * sin(2 * PI * freqs[noteIdx] * (i % (44100 / 2)) / 44100.0)
            val sample = (v * 32767).toInt().coerceIn(-32768, 32767)
            out.writeByte(sample and 0xFF)
            out.writeByte((sample shr 8) and 0xFF)
        }
        out.close()
        val wav = File(tempDir, "$name.wav")
        WavFileWriter.writePcmToWav(pcm, wav, 44100, 1, 16)
        return WavFileSource(wav)
    }

    @Test
    fun `same input produces identical results`() {
        val source = wavSource("scale-repeat")
        val r1 = runBlocking { useCase(source) }
        val r2 = runBlocking { useCase(source) }

        assertEquals(r1.vocalRange?.stableLowestMidi, r2.vocalRange?.stableLowestMidi, "稳定最低音应一致")
        assertEquals(r1.vocalRange?.stableHighestMidi, r2.vocalRange?.stableHighestMidi, "稳定最高音应一致")
        assertEquals(r1.comfortRange?.comfortLowestMidi, r2.comfortRange?.comfortLowestMidi, "舒适区最低应一致")
        assertEquals(r1.confidenceLevel, r2.confidenceLevel, "置信度分档应一致")
        assertEquals(r1.algorithmVersion, r2.algorithmVersion)
    }
}
