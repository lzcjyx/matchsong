package matchsong.app.perf

import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import matchsong.core.audio.analysis.AnalyzeRecordingUseCase
import matchsong.core.audio.api.WavFileSource
import matchsong.core.testing.WavTestFileFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * M10.1 设备端性能基准（真机/模拟器，SPEC §11 目标对照）。
 *
 * 非门禁（性能数据记录用，宽松断言防波动）：30s 音频分析耗时（目标 ≤10s 中端）
 * 与分析期进程内存（目标 ≤200MB 峰值）。
 *
 * 结果记录至 docs/experiments/m10-baselines.md（含设备/API/日期）。
 */
@RunWith(AndroidJUnit4::class)
class PerfBaselineTest {
    private val tag = "MatchSong:Perf"

    @Test
    fun baseline30sAnalysisTimeAndMemory() {
        val wav =
            WavTestFileFactory.writeToFile(
                file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "perf30.wav"),
                durationSec = 30.0,
                signalType = WavTestFileFactory.SignalType.SINE,
                frequencyHz = 220.0,
                amplitude = 0.5,
            )
        val useCase = AnalyzeRecordingUseCase()

        // 预热（JIT/分配热身）
        runBlocking { useCase(WavFileSource(wav)) }

        val start = System.nanoTime()
        val result = runBlocking { useCase(WavFileSource(wav)) }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        val mem = Debug.MemoryInfo()
        Debug.getMemoryInfo(mem)
        val elapsedText = "%.1f".format(elapsedMs)
        Log.i(
            tag,
            "30s分析=${elapsedText}ms PSS=${mem.totalPss}KB " +
                "native=${mem.nativePss}KB dalvik=${mem.dalvikPss}KB 质量可用=${result.qualityUsable}",
        )

        assertTrue("30s 分析应 <10s（SPEC 中端目标），实际 $elapsedText", elapsedMs < 10_000)
        assertTrue(result.qualityUsable)
    }
}
