package matchsong.domain.analysis

import kotlin.math.pow

/**
 * M5.3-2 稳定音域估计器（FR-ANAL-3/8，data-model §2.6，ACC-9）。
 *
 * 流程：
 * 1. 输入后处理轨迹（领域视图 [PitchAnalysisTrack]，组装层映射自 core:audio 输出）；
 * 2. 样本充足性门禁：有效帧数 ≥ A-5（120 帧 [推测]）→ 充足；不足时
 *    stableLowest/stableHighest/rangeSpan = null + sampleSufficiency=false +
 *    warning=INSUFFICIENT_SAMPLES（不输出音域，FR-ANAL-8）；
 * 3. 充足时：P5/P95 → 稳定最低/最高音（异常值稳健，非全帧极值）；
 *    rangeSpanSemitones = P95 − P5；coverage（帧落入稳定区间比例）；
 * 4. confidence [推测]（M5.8 标定，随 methodVersion 记录）：
 *    confidence = countFactor × compactness
 *    - countFactor = min(1, n / (2×MIN_VOICED_FRAMES))：帧数越多越可信，240 帧饱和；
 *    - compactness = 0.5^(IQR / 12)：IQR（P75−P25，半音）越宽分布越分散，
 *      一个八度 IQR → 0.5；
 *    - confidence < A-6 LOW（0.5）→ warning=LOW_CONFIDENCE（SPEC §13）。
 *
 * 纯 Kotlin 对象，零 Android 依赖。
 */
object VocalRangeEstimator {
    /** 算法版本（语义化：分位数参数 + 置信度公式版本，data-model §5.2）。 */
    const val METHOD_VERSION = "1.0.0"

    /**
     * 估计稳定音域。
     *
     * @param track 后处理后的音高轨迹（领域视图）
     * @return [VocalRangeEstimate]
     */
    fun estimate(track: PitchAnalysisTrack): VocalRangeEstimate {
        val voiced = track.voicedFrames
        val midis = voiced.map { it.midiNote }.filter { it.isFinite() }
        val sufficient =
            voiced.size >= AnalysisConfig.MIN_VOICED_FRAMES &&
                midis.size >= AnalysisConfig.MIN_VOICED_FRAMES
        if (!sufficient) {
            // FR-ANAL-8：数据不足不推断，音域字段全部为 null
            return VocalRangeEstimate(
                stableLowestMidi = null,
                stableHighestMidi = null,
                rangeSpanSemitones = null,
                coverage = 0.0,
                confidence = 0.0,
                sampleSufficiency = false,
                warning = AnalysisWarning.INSUFFICIENT_SAMPLES,
            )
        }
        val (low, high) = RangeStatistics.stableRange(midis)
        val coverage = RangeStatistics.coverage(midis, low, high)
        val confidence = confidence(midis, voiced.size)
        val warning =
            if (confidence < AnalysisConfig.LOW_CONFIDENCE_THRESHOLD) {
                AnalysisWarning.LOW_CONFIDENCE
            } else {
                null
            }
        return VocalRangeEstimate(
            stableLowestMidi = low,
            stableHighestMidi = high,
            rangeSpanSemitones = high - low,
            coverage = coverage,
            confidence = confidence,
            sampleSufficiency = true,
            warning = warning,
        )
    }

    /**
     * 置信度 [推测]：帧数因子 × 分布紧凑度（公式见类注释）。
     */
    private fun confidence(
        midis: List<Double>,
        voicedFrameCount: Int,
    ): Double {
        val countFactor =
            (voicedFrameCount / AnalysisConfig.CONFIDENCE_COUNT_SATURATION_FRAMES).coerceAtMost(1.0)
        val iqr = RangeStatistics.iqr(midis)
        val compactness =
            0.5.pow(iqr / AnalysisConfig.CONFIDENCE_COMPACTNESS_HALF_LIFE_SEMITONES)
        return (countFactor * compactness).coerceIn(0.0, 1.0)
    }
}
