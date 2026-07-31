package matchsong.domain.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M5.3-2 稳定音域估计器测试（FR-ANAL-3/8，ACC-9）。
 */
class VocalRangeEstimatorTest {
    private fun track(midis: List<Double>): PitchAnalysisTrack =
        PitchAnalysisTrack(
            midis.mapIndexed { i, m ->
                PitchAnalysisFrame(
                    timestampMs = i.toLong() * AnalysisConfig.FRAME_PERIOD_MS,
                    midiNote = m,
                    isVoiced = true,
                )
            },
        )

    @Test
    fun `sufficient concentrated samples produce full estimate`() {
        val midis = List(50) { 59.0 } + List(50) { 60.0 } + List(50) { 61.0 }
        val result = VocalRangeEstimator.estimate(track(midis))

        assertTrue(result.sampleSufficiency)
        assertNull(result.warning)
        assertEquals(59.0, result.stableLowestMidi!!, 1e-9)
        assertEquals(61.0, result.stableHighestMidi!!, 1e-9)
        assertEquals(2.0, result.rangeSpanSemitones!!, 1e-9)
        assertEquals(1.0, result.coverage, 1e-9)
        // countFactor = 150/240 = 0.625，compactness = 0.5^(2/12) ≈ 0.891 → ≈ 0.557
        assertTrue(result.confidence in 0.5..0.7, "实际 ${result.confidence}")
        assertEquals(0.05, result.lowQuantile, 1e-9)
        assertEquals(0.95, result.highQuantile, 1e-9)
        assertEquals("1.0.0", result.methodVersion)
    }

    @Test
    fun `tiny sample fails sufficiency gate with null range (ACC-9)`() {
        // FIX-TALK-150 类：有效帧远低于阈值
        val result = VocalRangeEstimator.estimate(track(List(50) { 60.0 }))

        assertFalse(result.sampleSufficiency)
        assertNull(result.stableLowestMidi)
        assertNull(result.stableHighestMidi)
        assertNull(result.rangeSpanSemitones)
        assertEquals(AnalysisWarning.INSUFFICIENT_SAMPLES, result.warning)
        assertEquals(0.0, result.confidence, 1e-9)
        assertEquals(0.0, result.coverage, 1e-9)
    }

    @Test
    fun `boundary frame count equals threshold is sufficient`() {
        val atThreshold =
            VocalRangeEstimator.estimate(
                track(List(AnalysisConfig.MIN_VOICED_FRAMES) { 60.0 }),
            )
        assertTrue(atThreshold.sampleSufficiency)
        assertEquals(60.0, atThreshold.stableLowestMidi!!, 1e-9)
        assertEquals(60.0, atThreshold.stableHighestMidi!!, 1e-9)
        assertEquals(0.0, atThreshold.rangeSpanSemitones!!, 1e-9)
        // 边界帧数：countFactor = 0.5、IQR=0 → confidence = 0.5，不触发 LOW
        assertNull(atThreshold.warning)

        val oneBelow =
            VocalRangeEstimator.estimate(
                track(List(AnalysisConfig.MIN_VOICED_FRAMES - 1) { 60.0 }),
            )
        assertFalse(oneBelow.sampleSufficiency)
        assertEquals(AnalysisWarning.INSUFFICIENT_SAMPLES, oneBelow.warning)
    }

    @Test
    fun `wide spread distribution yields LOW_CONFIDENCE warning`() {
        val midis = (0 until 240).map { 36.0 + it * 48.0 / 239.0 }
        val result = VocalRangeEstimator.estimate(track(midis))

        assertTrue(result.sampleSufficiency)
        assertEquals(AnalysisWarning.LOW_CONFIDENCE, result.warning)
        assertTrue(result.confidence < 0.5, "实际 ${result.confidence}")
        assertTrue(result.stableLowestMidi!! > 36.0)
        assertTrue(result.stableHighestMidi!! < 84.0)
        assertEquals(0.0, result.stableHighestMidi!! - result.stableLowestMidi!! - result.rangeSpanSemitones!!, 1e-9)
    }
}
