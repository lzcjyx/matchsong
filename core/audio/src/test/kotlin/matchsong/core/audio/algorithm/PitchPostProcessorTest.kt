package matchsong.core.audio.algorithm

import matchsong.core.audio.api.PitchFrame
import matchsong.core.audio.api.PitchTrack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * M5.2-1 后处理管线测试（FR-ANAL-2）。
 */
class PitchPostProcessorTest {
    private val processor = PitchPostProcessor()

    private fun frameAt(
        ms: Long,
        f0: Double,
        confidence: Double = 0.9,
    ): PitchFrame =
        PitchFrame(
            timestampMs = ms,
            f0Hz = f0,
            midiNote = PitchNotation.freqToMidi(f0),
            confidence = confidence,
            rms = 0.3,
            isVoiced = true,
        )

    private fun trackOf(frames: List<PitchFrame>): PitchTrack =
        PitchTrack(frames, frames.count { it.isVoiced }, "1.0.0")

    @Test
    fun `low confidence frames are dropped`() {
        val frames =
            listOf(
                frameAt(0, 440.0, confidence = 0.9),
                // 低置信 → 丢弃
                frameAt(200, 445.0, confidence = 0.2),
                frameAt(400, 441.0, confidence = 0.9),
            )
        val result = processor.process(trackOf(frames))
        assertEquals(2, result.frames.size, "低置信帧应被过滤")
    }

    @Test
    fun `octave error is corrected`() {
        // 440 → 880（八度错误）→ 440（修正回 880/2）
        val frames =
            listOf(
                frameAt(0, 440.0),
                frameAt(200, 880.0),
                frameAt(400, 442.0),
            )
        val result = processor.process(trackOf(frames))
        // 中值滤波后中间帧应为 ~440（修正后的 880/2 与前后中值）
        val middle = result.frames[1]
        assertTrue(middle.f0Hz < 500.0, "八度错误应被修正，实际 ${middle.f0Hz}")
    }

    @Test
    fun `transient jump is filtered`() {
        // 440 稳定序列中插入一个 700Hz 孤立跳变 → 丢弃
        val frames = (0..4).map { i -> frameAt(i * 200L, if (i == 2) 700.0 else 440.0) }
        val result = processor.process(trackOf(frames))
        val hasJump = result.frames.any { it.f0Hz > 600.0 }
        assertTrue(!hasJump, "孤立跳变帧应被过滤，实际 ${result.frames.map { it.f0Hz }}")
    }

    @Test
    fun `short segment is dropped`() {
        // 主要 440 中夹一段极短 500Hz（< 300ms）→ 丢弃
        val frames =
            (0..9).map { i ->
                val f0 = if (i in 3..4) 500.0 else 440.0
                frameAt(i * 100L, f0)
            }
        val result = processor.process(trackOf(frames))
        val has500 = result.frames.any { it.f0Hz > 480.0 }
        assertTrue(!has500, "短稳定片段应被丢弃，实际 ${result.frames.map { it.f0Hz }}")
    }

    @Test
    fun `stable track passes through`() {
        val frames = (0..9).map { i -> frameAt(i * 200L, 440.0) }
        val result = processor.process(trackOf(frames))
        assertEquals(10, result.frames.size)
        assertTrue(result.frames.all { abs(it.f0Hz - 440.0) < 20.0 })
    }
}
