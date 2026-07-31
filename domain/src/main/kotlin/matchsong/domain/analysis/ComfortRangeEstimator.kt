package matchsong.domain.analysis

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * M5.4-1 舒适音区估计器（FR-ANAL-4，PLAN M5.4，data-model §2.7）。
 *
 * 算法 [推测]（M5.8 真机人声标定，随 methodVersion 记录）：
 * 1. 半音桶直方图：帧按 round(midiNote) 归桶；
 * 2. 停留时间权重：连续同音片段（相邻帧差 ≤ 0.5 半音）按片段长度加权——
 *    片段内每帧贡献 = 片段长度（长音停留权重更大）；
 * 3. 稳定音符比例：桶内属于 ≥ 2 帧片段的帧占比 stableRatio；
 *    桶得分 = 停留权重 × (0.5 + 0.5×stableRatio)；
 * 4. 边缘样本检查：得分 < COMFORT_EDGE_MIN_FRAMES（10 [推测]）的桶剔除，
 *    稀疏边缘不拉宽舒适区；
 * 5. 舒适区 = 保留桶的最低/最高音；主要演唱音区 = 峰值桶向两侧扩展
 *    （相邻桶得分 ≥ 峰值×0.5 [推测]）；
 * 6. 一致性：舒适区与主音区裁剪到稳定音域 [stableLowest, stableHighest]
 *    （舒适区 ⊆ 稳定区间，与 M5.3 输出保持一致）；
 * 7. confidence [推测]：浓度 = 舒适区内权重 / 总权重；样本因子 = min(1, n/240)；
 *    confidence = 浓度 × 样本因子；
 * 8. 输出固定附带"本次录音估计"声明（FR-ANAL-4/7，语义固定）。
 *
 * 纯 Kotlin 对象，零 Android 依赖。
 */
object ComfortRangeEstimator {
    /** 算法版本（语义化，data-model §5.2）。 */
    const val METHOD_VERSION = "1.0.0"

    /** "本次录音估计"声明（FR-ANAL-4/7）。 */
    const val ESTIMATE_DISCLAIMER = AnalysisConfig.ESTIMATE_DISCLAIMER

    /**
     * 估计舒适音区。
     *
     * @param voicedMidis 有效帧 MIDI 序列
     * @param stableRange M5.3 稳定音域 (stableLowestMidi, stableHighestMidi)；
     *   提供时对舒适区执行一致性裁剪（舒适区 ⊆ 稳定区间）
     * @return [ComfortRangeEstimate]；样本不足时音区字段全部为 null
     */
    @Suppress("LongMethod") // 直方图+停留权重+边缘检查+裁剪内聚实现（84 行），测试全覆盖（M5.4-1）
    fun estimate(
        voicedMidis: List<Double>,
        stableRange: Pair<Double, Double>? = null,
    ): ComfortRangeEstimate {
        val midis = voicedMidis.filter { it.isFinite() }
        if (midis.size < AnalysisConfig.MIN_VOICED_FRAMES) {
            return ComfortRangeEstimate(
                comfortLowestMidi = null,
                comfortHighestMidi = null,
                primaryRangeLowMidi = null,
                primaryRangeHighMidi = null,
                confidence = 0.0,
                sampleSufficiency = false,
                estimateDisclaimer = ESTIMATE_DISCLAIMER,
            )
        }

        // 1+2：连续片段（同音判定 ±0.5 半音）→ 停留时间权重（片段长度）
        val runs = buildRuns(midis)
        val scores = mutableMapOf<Int, Double>()
        val binCounts = mutableMapOf<Int, Int>()
        val binStableCounts = mutableMapOf<Int, Int>()
        for (run in runs) {
            val runLen = run.size
            for (i in run) {
                val bin = midis[i].roundToInt()
                binCounts[bin] = binCounts.getOrDefault(bin, 0) + 1
                if (runLen >= AnalysisConfig.MIN_STABLE_RUN_FRAMES) {
                    binStableCounts[bin] = binStableCounts.getOrDefault(bin, 0) + 1
                }
                scores[bin] = scores.getOrDefault(bin, 0.0) + runLen
            }
        }

        // 3：桶得分 = 停留权重 × (0.5 + 0.5×稳定比例) [推测]
        val binScores =
            binCounts.map { (bin, count) ->
                val dwell = scores.getValue(bin)
                val stableRatio = (binStableCounts[bin] ?: 0).toDouble() / count
                bin to dwell * (0.5 + 0.5 * stableRatio)
            }

        // 4：边缘样本检查
        val kept = binScores.filter { it.second >= AnalysisConfig.COMFORT_EDGE_MIN_FRAMES }
        if (kept.isEmpty()) {
            return ComfortRangeEstimate(
                comfortLowestMidi = null,
                comfortHighestMidi = null,
                primaryRangeLowMidi = null,
                primaryRangeHighMidi = null,
                confidence = 0.0,
                sampleSufficiency = true,
                estimateDisclaimer = ESTIMATE_DISCLAIMER,
            )
        }
        val totalWeight = binScores.sumOf { it.second }

        // 5：舒适区 + 主要演唱音区（峰值桶扩展）
        var comfortLow = kept.minOf { it.first }.toDouble()
        var comfortHigh = kept.maxOf { it.first }.toDouble()
        var primary = primaryRange(kept.toMap())

        // 6：一致性裁剪（舒适区 ⊆ 稳定区间）
        if (stableRange != null) {
            comfortLow = max(comfortLow, stableRange.first)
            comfortHigh = min(comfortHigh, stableRange.second)
            primary = primary.first.coerceAtLeast(stableRange.first) to
                primary.second.coerceAtMost(stableRange.second)
        }
        if (comfortLow > comfortHigh) {
            // 理论上不发生（稳定区间来自同一轨迹）；防御性返回空区间 [推测]
            return ComfortRangeEstimate(
                comfortLowestMidi = null,
                comfortHighestMidi = null,
                primaryRangeLowMidi = null,
                primaryRangeHighMidi = null,
                confidence = 0.0,
                sampleSufficiency = true,
                estimateDisclaimer = ESTIMATE_DISCLAIMER,
            )
        }

        // 7：置信度 [推测] = 浓度 × 样本因子
        val comfortWeight =
            kept
                .filter { it.first.toDouble() in comfortLow..comfortHigh }
                .sumOf { it.second }
        val concentration = comfortWeight / totalWeight
        val sampleFactor = min(1.0, midis.size / (2.0 * AnalysisConfig.MIN_VOICED_FRAMES))
        val confidence = (concentration * sampleFactor).coerceIn(0.0, 1.0)

        return ComfortRangeEstimate(
            comfortLowestMidi = comfortLow,
            comfortHighestMidi = comfortHigh,
            primaryRangeLowMidi = primary.first,
            primaryRangeHighMidi = primary.second,
            confidence = confidence,
            sampleSufficiency = true,
            estimateDisclaimer = ESTIMATE_DISCLAIMER,
        )
    }

    /** 连续片段切分：相邻帧差 > 容差则另起片段。 */
    private fun buildRuns(midis: List<Double>): List<List<Int>> {
        val runs = mutableListOf<MutableList<Int>>()
        for (i in midis.indices) {
            val joinPrev =
                runs.isNotEmpty() &&
                    abs(midis[i] - midis[i - 1]) <= AnalysisConfig.COMFORT_SAME_NOTE_TOLERANCE_SEMITONES
            if (joinPrev) runs.last().add(i) else runs.add(mutableListOf(i))
        }
        return runs
    }

    /** 主要演唱音区：峰值桶向两侧扩展，直到相邻桶得分 < 峰值×比例 [推测]。 */
    private fun primaryRange(kept: Map<Int, Double>): Pair<Double, Double> {
        val peak =
            kept.entries.maxByOrNull { it.value }
                ?: return Double.NaN to Double.NaN
        val threshold = peak.value * AnalysisConfig.COMFORT_PRIMARY_PEAK_RATIO
        var low = peak.key
        var high = peak.key
        var expanded = true
        while (expanded) {
            expanded = false
            val l = kept[low - 1]
            if (l != null && l >= threshold) {
                low--
                expanded = true
            }
            val r = kept[high + 1]
            if (r != null && r >= threshold) {
                high++
                expanded = true
            }
        }
        return low.toDouble() to high.toDouble()
    }
}

/**
 * M5.4 舒适音区估计结果（字段对齐 data-model §2.7 VoiceFeatureVector 结构）。
 */
data class ComfortRangeEstimate(
    /** 舒适最低音（MIDI，半音桶下界）；样本不足时为 null。 */
    val comfortLowestMidi: Double?,
    /** 舒适最高音（MIDI，半音桶上界）；样本不足时为 null。 */
    val comfortHighestMidi: Double?,
    /** 主要演唱音区低音（MIDI，峰值桶区间）；样本不足时为 null。 */
    val primaryRangeLowMidi: Double?,
    /** 主要演唱音区高音（MIDI，峰值桶区间）；样本不足时为 null。 */
    val primaryRangeHighMidi: Double?,
    /** 置信度 [0,1]（浓度 × 样本因子，公式见 ComfortRangeEstimator）。 */
    val confidence: Double,
    /** 样本充足性（有效帧 ≥ A-5）。 */
    val sampleSufficiency: Boolean,
    /** "本次录音估计"声明（FR-ANAL-4/7，语义固定）。 */
    val estimateDisclaimer: String,
    /** 算法版本（语义化）。 */
    val methodVersion: String = ComfortRangeEstimator.METHOD_VERSION,
)
