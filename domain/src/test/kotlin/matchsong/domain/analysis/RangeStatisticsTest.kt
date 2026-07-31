package matchsong.domain.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M5.3-1 稳定音域统计核心测试（FR-ANAL-3，PLAN M5.3）。
 */
class RangeStatisticsTest {
    @Test
    fun `Type-7 percentile matches hand calculation`() {
        val sorted = listOf(1.0, 2.0, 3.0, 4.0)
        assertEquals(1.75, RangeStatistics.percentile(sorted, 0.25), 1e-9)
        assertEquals(2.5, RangeStatistics.percentile(sorted, 0.5), 1e-9)
        assertEquals(3.25, RangeStatistics.percentile(sorted, 0.75), 1e-9)

        val seq = (1..100).map { it.toDouble() }
        assertEquals(5.95, RangeStatistics.percentile(seq, 0.05), 1e-9)
        assertEquals(50.5, RangeStatistics.percentile(seq, 0.5), 1e-9)
        assertEquals(95.05, RangeStatistics.percentile(seq, 0.95), 1e-9)
    }

    @Test
    fun `percentile handles degenerate inputs`() {
        assertTrue(RangeStatistics.percentile(emptyList(), 0.5).isNaN())
        assertEquals(60.0, RangeStatistics.percentile(listOf(60.0), 0.05), 1e-9)
        assertEquals(60.0, RangeStatistics.percentile(listOf(60.0), 0.95), 1e-9)
    }

    @Test
    fun `stable range is unaffected by outliers while naive min max is`() {
        // 88 帧集中在 58..62，外加 40/80 各 2 帧离群
        val bulk = (0 until 88).map { 58.0 + it * 4.0 / 87.0 }
        val midis = bulk + listOf(40.0, 40.0, 80.0, 80.0)

        val naiveMin = midis.minOrNull()!!
        val naiveMax = midis.maxOrNull()!!
        assertEquals(40.0, naiveMin)
        assertEquals(80.0, naiveMax)

        val (low, high) = RangeStatistics.stableRange(midis)
        // P5/P95 应远离离群极值（对照：极值法 [40, 80] vs 分位窗 [≈58, ≈62]）
        assertTrue(low > 50.0, "P5 不应被离群极值拉低，实际 $low")
        assertTrue(high < 70.0, "P95 不应被离群极值拉高，实际 $high")
        assertEquals(58.0, low, 0.2)
        assertEquals(62.0, high, 0.2)
        assertTrue(low >= naiveMin && high <= naiveMax)
    }

    @Test
    fun `stable range of empty input is NaN`() {
        val (low, high) = RangeStatistics.stableRange(emptyList())
        assertTrue(low.isNaN() && high.isNaN())
    }

    @Test
    fun `coverage counts frames inside quantile window`() {
        val seq = (1..100).map { it.toDouble() }
        val p5 = RangeStatistics.percentile(seq, 0.05) // 5.95
        val p95 = RangeStatistics.percentile(seq, 0.95) // 95.05
        assertEquals(0.90, RangeStatistics.coverage(seq, p5, p95), 1e-9)

        assertEquals(1.0, RangeStatistics.coverage(List(100) { 60.0 }, 60.0, 60.0), 1e-9)
        assertEquals(0.0, RangeStatistics.coverage(emptyList(), 0.0, 100.0), 1e-9)
    }

    @Test
    fun `iqr measures distribution spread`() {
        assertEquals(0.0, RangeStatistics.iqr(List(50) { 60.0 }), 1e-9)
        assertEquals(2.0, RangeStatistics.iqr(List(50) { 59.0 } + List(50) { 60.0 } + List(50) { 61.0 }), 1e-9)
        assertTrue(RangeStatistics.iqr(emptyList()).isNaN())
    }
}
