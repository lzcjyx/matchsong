package matchsong.domain.analysis

import kotlin.math.ceil
import kotlin.math.floor

/**
 * M5.3-1 稳定音域统计核心（FR-ANAL-3，PLAN M5.3）。
 *
 * 异常值剔除策略 [推测]：**不直接取全部帧的极值**（PLAN M5.3 明确禁止），而是以
 * 分位数窗口（默认 P5/P95，data-model A-4，可配置）作为稳健区间——窗口外的帧天然
 * 视为离群，窗口内计算稳定最低/最高音；[coverage] 报告落入窗口内的帧占比，供
 * "本次录音覆盖范围"解释（ACC-10）。
 *
 * 分位数算法：Type-7 线性插值（NumPy 默认 / Hyndman-Fan 第 7 型）：
 * h = (n-1)·p，x = sorted[⌊h⌋] + (h − ⌊h⌋)·(sorted[⌈h⌉] − sorted[⌊h⌋])。
 *
 * 纯函数，零 Android 依赖。
 */
object RangeStatistics {
    /**
     * Type-7 线性插值分位数。
     *
     * @param sorted 升序排列的样本（调用方保证有序）
     * @param p 分位点 (0, 1]
     * @return 分位数；空输入返回 NaN
     */
    fun percentile(
        sorted: List<Double>,
        p: Double,
    ): Double {
        if (sorted.isEmpty()) return Double.NaN
        val n = sorted.size
        val h = (n - 1) * p
        val lo = floor(h).toInt()
        val hi = ceil(h).toInt()
        val lower = sorted[lo]
        val upper = sorted[hi]
        return lower + (h - lo) * (upper - lower)
    }

    /**
     * 稳定音域 [推测]：P5/P95 分位窗口（异常值稳健，非全帧极值）。
     *
     * @return (stableLowest, stableHighest)；空输入返回 (NaN, NaN)
     */
    fun stableRange(midis: List<Double>): Pair<Double, Double> {
        if (midis.isEmpty()) return Double.NaN to Double.NaN
        val sorted = midis.sorted()
        return percentile(sorted, AnalysisConfig.LOW_QUANTILE) to
            percentile(sorted, AnalysisConfig.HIGH_QUANTILE)
    }

    /**
     * 覆盖范围：有效帧落入 [p5, p95] 区间的比例（data-model §2.6 coverage）。
     *
     * @return [0, 1]；空输入返回 0.0
     */
    fun coverage(
        midis: List<Double>,
        p5: Double,
        p95: Double,
    ): Double {
        if (midis.isEmpty()) return 0.0
        val inside = midis.count { it >= p5 && it <= p95 }
        return inside.toDouble() / midis.size
    }

    /**
     * 四分位距 IQR（P75 − P25，半音）——M5.3-2 置信度分布紧凑度用 [推测]。
     *
     * @return IQR；空输入返回 NaN
     */
    fun iqr(midis: List<Double>): Double {
        if (midis.isEmpty()) return Double.NaN
        val sorted = midis.sorted()
        return percentile(sorted, 0.75) - percentile(sorted, 0.25)
    }
}
