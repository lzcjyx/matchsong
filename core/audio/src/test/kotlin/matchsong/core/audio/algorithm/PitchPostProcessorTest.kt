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
        // 真实帧周期 46ms；12 帧 ≈ 552ms（≥ 300ms 最短片段，避免被时长规则误删）
        val frames =
            (0..11).map { i ->
                val f0 = if (i == 5) 445.0 else 440.0
                val conf = if (i == 5) 0.2 else 0.9
                frameAt(i * 46L, f0, confidence = conf)
            }
        val result = processor.process(trackOf(frames))
        assertEquals(11, result.frames.size, "低置信帧应被过滤")
    }

    @Test
    fun `octave error is corrected`() {
        // 440 → 880（八度错误段，460ms）→ 440；修正回 880/2
        val frames =
            (0..19).map { i ->
                val f0 = if (i in 5..14) 880.0 else 440.0
                frameAt(i * 46L, f0)
            }
        val result = processor.process(trackOf(frames))
        // 八度修正 + 中值滤波后无 >500Hz 帧；整轨 920ms 单段保留
        assertTrue(result.frames.isNotEmpty(), "修正后帧不应被时长规则误删")
        assertTrue(
            result.frames.all { it.f0Hz < 500.0 },
            "八度错误应被修正，实际 ${result.frames.map { it.f0Hz }} 等",
        )
    }

    @Test
    fun `transient jump is filtered`() {
        // 真实帧周期 46ms；440 稳定序列中插入一个 700Hz 孤立跳变 → 丢弃
        val frames =
            (0..11).map { i -> frameAt(i * 46L, if (i == 5) 700.0 else 440.0) }
        val result = processor.process(trackOf(frames))
        val hasJump = result.frames.any { it.f0Hz > 600.0 }
        assertTrue(result.frames.size >= 8, "跳变帧外的稳定段应保留（≥300ms）")
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
        val frames = (0..9).map { i -> frameAt(i * 46L, 440.0) }
        val result = processor.process(trackOf(frames))
        assertEquals(10, result.frames.size)
        assertTrue(result.frames.all { abs(it.f0Hz - 440.0) < 20.0 })
    }

    @Test
    fun `short speech-like segment separated by gap is dropped even when pitch matches singing`() {
        // BUG-015：说话词（<300ms）与歌唱音同音高且相邻——旧逻辑按音高合并成长片段逃过滤；
        // 新逻辑按时间间隔（>150ms）分段，说话词被丢弃、歌唱段保留
        val frames =
            listOf(
                // 说话词：4 帧 ≈ 184ms @ 220Hz（< 300ms 最短片段）
                frameAt(0, 220.0),
                frameAt(46, 220.0),
                frameAt(92, 220.0),
                frameAt(138, 220.0),
                // 停顿 200ms（> maxSegmentGapMs=150ms，分段）
                frameAt(338, 220.0),
                frameAt(384, 220.0),
                frameAt(430, 220.0),
                frameAt(476, 220.0),
                frameAt(522, 220.0),
                frameAt(568, 220.0),
                frameAt(614, 220.0),
                frameAt(660, 220.0),
                frameAt(706, 220.0),
                frameAt(752, 220.0),
            )
        val result = processor.process(trackOf(frames))
        // 说话段（184ms）被丢弃；歌唱段（12 帧 ≈ 414ms）保留
        assertTrue(result.frames.size < frames.size, "短说话段应被丢弃")
        assertTrue(result.frames.size >= 8, "长歌唱段应保留")
    }
}
