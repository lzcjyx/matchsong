package matchsong.domain.analysis

/**
 * M5.3 稳定音域估计结果（data-model §2.6 VocalRangeEstimate）。
 *
 * 说明：estimateId/sessionId 为系统与持久化层字段（UUID，随 VoiceFeatureVector
 * 落库），不在分析层生成；lowQuantile/highQuantile 为配置快照，随结果追溯。
 */
data class VocalRangeEstimate(
    /** 稳定最低音（MIDI，P5）；样本不足时为 null（FR-ANAL-8）。 */
    val stableLowestMidi: Double?,
    /** 稳定最高音（MIDI，P95）；样本不足时为 null。 */
    val stableHighestMidi: Double?,
    /** 音域跨度（半音 = stableHighest − stableLowest）；样本不足时为 null。 */
    val rangeSpanSemitones: Double?,
    /** 覆盖范围 [0,1]：有效音高帧落入稳定区间的比例（data-model §2.6）。 */
    val coverage: Double,
    /** 置信度 [0,1]（分布紧凑度 × 帧数因子，公式见 VocalRangeEstimator）。 */
    val confidence: Double,
    /** 样本充足性（有效帧 ≥ A-5 才输出正式结果，ACC-9）。 */
    val sampleSufficiency: Boolean,
    /** 警告：INSUFFICIENT_SAMPLES / LOW_CONFIDENCE；null = 无警告（等价 NONE）。 */
    val warning: AnalysisWarning?,
    /** 低分位（配置快照，默认 0.05，A-4）。 */
    val lowQuantile: Double = AnalysisConfig.LOW_QUANTILE,
    /** 高分位（配置快照，默认 0.95，A-4）。 */
    val highQuantile: Double = AnalysisConfig.HIGH_QUANTILE,
    /** 算法版本（语义化，含分位数参数，data-model §5.2）。 */
    val methodVersion: String = VocalRangeEstimator.METHOD_VERSION,
)

/**
 * 分析警告（data-model §2.7 warnings 枚举，M5.3-5 范围内的子集）。
 */
enum class AnalysisWarning {
    /** 样本不足（有效帧 < A-5，FR-ANAL-8/ACC-9）。 */
    INSUFFICIENT_SAMPLES,

    /** 置信度低于阈值（< A-6 LOW 分档，SPEC §13）。 */
    LOW_CONFIDENCE,

    /** 无警告（保留供列表语义/其他消费者使用；单值字段以 null 表达）。 */
    NONE,
}
