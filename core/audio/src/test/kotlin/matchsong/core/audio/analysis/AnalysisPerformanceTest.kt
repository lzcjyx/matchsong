package matchsong.core.audio.analysis

import kotlinx.coroutines.runBlocking
import matchsong.core.audio.algorithm.WavFileWriter
import matchsong.core.audio.api.WavFileSource
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * M5.8-2 分析性能冒烟（SPEC §11：30s 音频 ≤10s 中端设备）。
 *
 * 说明：JVM 桌面测量仅供参考（M10.1 真机基准定论）；
 * 记录到 docs/experiments/m5-performance.md。非门禁（性能波动不失败）。
 */
class AnalysisPerformanceTest {
    @TempDir
    lateinit var tempDir: File

    private val useCase = AnalyzeRecordingUseCase()

    private fun wavSource30s(): WavFileSource {
        val n = 44100 * 30
        val pcm = File(tempDir, "perf30.pcm")
        val out = java.io.DataOutputStream(pcm.outputStream())
        for (i in 0 until n) {
            val t = i.toDouble() / 44100.0
            val v = 0.5 * sin(2 * PI * 220.0 * t)
            val sample = (v * 32767).toInt().coerceIn(-32768, 32767)
            out.writeByte(sample and 0xFF)
            out.writeByte((sample shr 8) and 0xFF)
        }
        out.close()
        val wav = File(tempDir, "perf30.wav")
        WavFileWriter.writePcmToWav(pcm, wav, 44100, 1, 16)
        return WavFileSource(wav)
    }

    @Test
    fun `30 second analysis completes within budget`() {
        val source = wavSource30s()
        // 预热
        runBlocking { useCase(source) }
        val start = System.nanoTime()
        val result = runBlocking { useCase(source) }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        println("M5.8-2 性能：30s 音频端到端分析 ${"%.1f".format(elapsedMs)}ms（JVM 桌面，参考值）")
        // 桌面 JVM 应远低于 10s 预算；宽松断言（<5s）防 CI 波动
        assertTrue(elapsedMs < 5_000, "30s 分析应 <5s（JVM 参考），实际 ${"%.1f".format(elapsedMs)}ms")
        assertTrue(result.qualityUsable)
    }
}
