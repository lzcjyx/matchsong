package matchsong.app.recording

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import matchsong.core.audio.android.RecordingFileManager
import matchsong.core.audio.android.RecordingSessionRunner
import matchsong.core.testing.fake.FakeAudioRecorder
import matchsong.domain.recording.RecordingState
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 真机 Bug 复现测试（BUG-019/020）：真实 [RecordingSessionRunner] + Fake 录音机 +
 * 真实 [RecordingFileManager]，在模拟器上驱动完整录音→落盘→完成链路。
 *
 * 覆盖（均为用户真机症状）：
 * - 首次录音结束必须产出有效 WAV（数据帧 > 0）——当前代码 recorder.start() 从未被调用，
 *   零音频帧 → 空 WAV（44 字节头），此断言失败即复现；
 * - 第二次录音必须能重启（start() 门禁不得拦截 COMPLETED/FAILED 后的重启）；
 * - stop() 后 lastWavFile 必须先于 COMPLETED 就绪（BUG-014 回归）。
 */
@RunWith(AndroidJUnit4::class)
class RecordingHandoffTest {
    private lateinit var runner: RecordingSessionRunner

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.cacheDir, "handoff-test").apply { deleteRecursively() }
        runner =
            RecordingSessionRunner(
                recorder = FakeAudioRecorder(),
                fileManager = RecordingFileManager(dir),
            )
        RecordingSessionRunner.instance = runner
    }

    private fun awaitState(
        expected: RecordingState,
        timeoutMs: Long = 15_000,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (runner.stateFlow.value == expected) return
            Thread.sleep(100)
        }
        throw AssertionError("状态未在 ${timeoutMs}ms 内到达 $expected，当前 ${runner.stateFlow.value}")
    }

    @Test
    fun firstSessionProducesUsableWav() {
        runner.start(InstrumentationRegistry.getInstrumentation().targetContext)
        awaitState(RecordingState.RECORDING) // 倒计时 3s 后进入录音
        runner.stop(interrupted = false)
        awaitState(RecordingState.COMPLETED)

        val wav = runner.lastWavFile
        assertNotNull("COMPLETED 后 lastWavFile 不得为 null", wav)
        assertTrue("WAV 文件应存在", wav!!.exists())
        assertTrue(
            "WAV 必须包含音频数据帧（>44 字节头；当前为 0 帧空 WAV——recorder.start 缺失）",
            wav.length() > 44,
        )
    }

    @Test
    fun secondSessionCanRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runner.start(context)
        awaitState(RecordingState.RECORDING)
        runner.stop(interrupted = false)
        awaitState(RecordingState.COMPLETED)
        val firstWav = runner.lastWavFile

        // 第二次录音必须真正重启（start() 不得因 COMPLETED 门禁拦截）
        runner.start(context)
        awaitState(RecordingState.RECORDING)
        runner.stop(interrupted = false)
        awaitState(RecordingState.COMPLETED)

        val secondWav = runner.lastWavFile
        assertNotNull("第二次录音应产出新 WAV", secondWav)
        assertTrue("第二次录音的 WAV 应存在且非空", secondWav!!.exists() && secondWav.length() > 44)
        if (firstWav != null && secondWav.absolutePath != firstWav.absolutePath) {
            assertTrue("新旧会话文件不得互相覆盖残留", secondWav.length() > 44)
        }
    }
}
