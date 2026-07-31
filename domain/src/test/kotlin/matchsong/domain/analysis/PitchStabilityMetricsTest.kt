package matchsong.domain.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M5.5-1 音高稳定性指标测试（FR-ANAL-5，PLAN M5.5）。
 */
class PitchStabilityMetricsTest {
    private fun voicedFrames(notes: List<Double>): List<PitchAnalysisFrame> =
        notes.mapIndexed { i, m ->
            PitchAnalysisFrame(
                timestampMs = i.toLong() * AnalysisConfig.FRAME_PERIOD_MS,
                midiNote = m,
                isVoiced = true,
            )
        }

    @Test
    fun `steady sine yields high stability and zero deviation`() {
        val voiced = voicedFrames(List(200) { 60.0 })
        val track =
            PitchAnalysisTrack(
                voiced +
                    List(20) {
                        PitchAnalysisFrame(
                            timestampMs = (200 + it).toLong() * AnalysisConfig.FRAME_PERIOD_MS,
                            midiNote = Double.NaN,
                            isVoiced = false,
                        )
                    },
            )
        val r = PitchStabilityMetrics.compute(track)

        assertEquals(1.0, r.stableFrameRatio, 1e-9)
        assertEquals(0.0, r.pitchDeviationCents, 1e-9)
        assertEquals(0.0, r.longNoteDeviationCents, 1e-9)
        assertEquals(200.0 / 220.0, r.voicedFrameRatio, 1e-9)
    }

    @Test
    fun `off-key alternation worsens stability metrics`() {
        // 60.0 / 61.0 交替（相差 100 音分）→ 全部孤立单帧片段，无稳定片段
        val notes = (0 until 200).map { if (it % 2 == 0) 60.0 else 61.0 }
        val r = PitchStabilityMetrics.compute(PitchAnalysisTrack(voicedFrames(notes)))

        assertEquals(0.0, r.stableFrameRatio, 1e-9)
        // 中位 6050 音分，每帧偏差 50 音分 → MAD = 50
        assertEquals(50.0, r.pitchDeviationCents, 1e-9)
        assertEquals(0.0, r.longNoteDeviationCents, 1e-9)
    }

    @Test
    fun `long note jitter is captured by longNoteDeviationCents`() {
        // 长音 25 帧 ≈ 1150ms ≥ 800ms，±10 音分抖动
        val longNote = (0 until 25).map { if (it % 2 == 0) 60.1 else 59.9 }
        // 短音 5 帧 ≈ 230ms < 800ms，即使抖动也不计入长音波动
        val shortNote = List(5) { 61.0 }
        val r = PitchStabilityMetrics.compute(PitchAnalysisTrack(voicedFrames(longNote + shortNote)))

        assertEquals(10.0, r.longNoteDeviationCents, 0.5)
        assertTrue(r.pitchDeviationCents > 0.0)
    }

    @Test
    fun `short notes only yield zero long note deviation`() {
        val short = List(5) { 61.0 }
        val r = PitchStabilityMetrics.compute(PitchAnalysisTrack(voicedFrames(short)))
        assertEquals(0.0, r.longNoteDeviationCents, 1e-9)
        assertEquals(1.0, r.stableFrameRatio, 1e-9)
    }

    @Test
    fun `gradual drift within 50 cents of local median stays stable`() {
        // 从 60.0 缓慢漂移到 ≈60.8（总漂移 80 音分），但相对片段中位数偏差 ≤ 50 音分
        val notes = (0 until 200).map { 60.0 + it * 0.004 }
        val r = PitchStabilityMetrics.compute(PitchAnalysisTrack(voicedFrames(notes)))
        assertTrue(r.stableFrameRatio > 0.9, "实际 ${r.stableFrameRatio}")
    }

    @Test
    fun `empty and all-unvoiced tracks yield zeros`() {
        val empty = PitchStabilityMetrics.compute(PitchAnalysisTrack(emptyList()))
        assertEquals(0.0, empty.stableFrameRatio, 1e-9)
        assertEquals(0.0, empty.pitchDeviationCents, 1e-9)
        assertEquals(0.0, empty.longNoteDeviationCents, 1e-9)
        assertEquals(0.0, empty.voicedFrameRatio, 1e-9)

        val allUnvoiced =
            PitchAnalysisTrack(
                List(10) {
                    PitchAnalysisFrame(it.toLong() * AnalysisConfig.FRAME_PERIOD_MS, Double.NaN, isVoiced = false)
                },
            )
        val r = PitchStabilityMetrics.compute(allUnvoiced)
        assertEquals(0.0, r.voicedFrameRatio, 1e-9)
        assertEquals(0.0, r.stableFrameRatio, 1e-9)
        assertEquals(0.0, r.pitchDeviationCents, 1e-9)
    }
}
