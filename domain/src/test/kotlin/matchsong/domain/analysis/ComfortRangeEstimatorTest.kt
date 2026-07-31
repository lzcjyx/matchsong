package matchsong.domain.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M5.4-1 舒适音区估计器测试（FR-ANAL-4，PLAN M5.4）。
 */
class ComfortRangeEstimatorTest {
    @Test
    fun `concentrated trajectory produces matching comfort range`() {
        // 每半音桶 20 帧连续（片段长 20 → 稳定比例 1），总 140 帧
        val midis = (57..63).flatMap { bin -> List(20) { bin.toDouble() } }
        val result = ComfortRangeEstimator.estimate(midis)

        assertTrue(result.sampleSufficiency)
        assertEquals(57.0, result.comfortLowestMidi!!, 1e-9)
        assertEquals(63.0, result.comfortHighestMidi!!, 1e-9)
        assertEquals(57.0, result.primaryRangeLowMidi!!, 1e-9)
        assertEquals(63.0, result.primaryRangeHighMidi!!, 1e-9)
        assertEquals(ComfortRangeEstimator.ESTIMATE_DISCLAIMER, result.estimateDisclaimer)
        assertEquals("本次录音估计", result.estimateDisclaimer)
        assertTrue(result.confidence > 0.5, "实际 ${result.confidence}")
    }

    @Test
    fun `sparse edge samples do not widen comfort range`() {
        val core = (57..63).flatMap { bin -> List(20) { bin.toDouble() } }
        // 边缘稀疏帧：50/51、70/71 交替各 8 帧 → 全部为孤立单帧片段，得分 2 < 阈值 10
        val lowEdge = (0 until 8).map { if (it % 2 == 0) 50.0 else 51.0 }
        val highEdge = (0 until 8).map { if (it % 2 == 0) 70.0 else 71.0 }
        val midis = lowEdge + core + highEdge

        val result = ComfortRangeEstimator.estimate(midis)
        assertTrue(result.sampleSufficiency)
        assertEquals(57.0, result.comfortLowestMidi!!, 1e-9)
        assertEquals(63.0, result.comfortHighestMidi!!, 1e-9)
        assertEquals(57.0, result.primaryRangeLowMidi!!, 1e-9)
        assertEquals(63.0, result.primaryRangeHighMidi!!, 1e-9)
    }

    @Test
    fun `long dwell raises weight of its semitone bin`() {
        // 60 帧长音（单片段长 60）+ 60 帧短促音符（孤立单帧）
        val sustained = List(60) { 60.0 }
        val shortNotes = (0 until 60).map { if (it % 2 == 0) 62.0 else 63.0 }
        val result = ComfortRangeEstimator.estimate(sustained + shortNotes)

        // 长音桶权重（3600）远高于短促桶（15），峰值即主音区
        assertEquals(60.0, result.primaryRangeLowMidi!!, 1e-9)
        assertEquals(60.0, result.primaryRangeHighMidi!!, 1e-9)
        assertEquals(60.0, result.comfortLowestMidi!!, 1e-9)
        assertEquals(63.0, result.comfortHighestMidi!!, 1e-9)
    }

    @Test
    fun `insufficient samples yield null comfort range`() {
        val result = ComfortRangeEstimator.estimate(List(50) { 60.0 })

        assertFalse(result.sampleSufficiency)
        assertNull(result.comfortLowestMidi)
        assertNull(result.comfortHighestMidi)
        assertNull(result.primaryRangeLowMidi)
        assertNull(result.primaryRangeHighMidi)
        assertEquals(0.0, result.confidence, 1e-9)
        assertEquals(ComfortRangeEstimator.ESTIMATE_DISCLAIMER, result.estimateDisclaimer)
    }

    @Test
    fun `comfort range is subset of stable range`() {
        // 11 桶 × 12 帧 = 132 帧，稳定音域 [57, 63]
        val midis = (55..65).flatMap { bin -> List(12) { bin.toDouble() } }
        val stable = 57.0 to 63.0
        val result = ComfortRangeEstimator.estimate(midis, stableRange = stable)

        assertTrue(result.sampleSufficiency)
        // 舒适区与主音区均被裁剪进稳定区间（一致性约束）
        assertEquals(57.0, result.comfortLowestMidi!!, 1e-9)
        assertEquals(63.0, result.comfortHighestMidi!!, 1e-9)
        assertEquals(57.0, result.primaryRangeLowMidi!!, 1e-9)
        assertEquals(63.0, result.primaryRangeHighMidi!!, 1e-9)
    }
}
