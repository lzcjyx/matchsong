package matchsong.core.audio.algorithm

/**
 * M4.4-1 质量警告类型（FR-QUAL-1，PLAN M4.4，data-model §2.3 对齐）。
 *
 * UI 层 QualityFailureReason（M2.2-3）与本枚举一一映射（M4.5-1）。
 */
enum class QualityWarning {
    /** 录音过短（< R-3 minDurationMs）。 */
    TOO_SHORT,

    /** 纯静音/无有效声音。 */
    SILENT,

    /** 音量过低。 */
    TOO_QUIET,

    /** 环境噪声过高。 */
    NOISY,

    /** 麦克风削波超限。 */
    CLIPPING,

    /** 有效演唱片段不足。 */
    INSUFFICIENT_VOICE,
}

/**
 * M4.4-1 音频质量报告（FR-QUAL-1，data-model §2.3 字段）。
 */
data class AudioQualityReport(
    /** 是否可进入分析（false → 不生成正式结果，FR-QUAL-3）。 */
    val isUsable: Boolean,
    /** 结果置信度（0.0..1.0；与 isUsable 解耦：可用但低置信）。 */
    val confidence: Double,
    /** 录音时长（毫秒）。 */
    val durationMs: Long,
    /** 静音帧比例（0.0..1.0）。 */
    val silenceRatio: Double,
    /** 低音量帧比例（0.0..1.0）。 */
    val quietRatio: Double,
    /** 削波帧比例（0.0..1.0）。 */
    val clippingRatio: Double,
    /** 平均 RMS（归一化 0.0..1.0）。 */
    val averageRms: Double,
    /** 峰值（归一化 0.0..1.0）。 */
    val peak: Double,
    /** 有效声音帧比例（0.0..1.0）。 */
    val activeRatio: Double,
    /** 近似噪声水平（低幅值帧 RMS 分位，[推测] data-model §2.3）。 */
    val noiseEstimate: Double,
    /** 可分析帧数量。 */
    val analyzableFrameCount: Int,
    /** 有效演唱活动区间（起止毫秒列表，[推测] data-model §2.3）。 */
    val vocalActivityRanges: List<Pair<Long, Long>>,
    /** 命中的警告（有序，按优先级；空 = 可用）。 */
    val warnings: List<QualityWarning>,
    /** 建议动作（UI 文案依据）。 */
    val recommendedAction: QualityAction,
    /** 质量检测算法版本（阈值版本，M4.6-2 标定后递增）。 */
    val qualityVersion: String,
)

/** 质量建议动作（M4.5-1 文案映射）。 */
enum class QualityAction {
    /** 可继续分析。 */
    ANALYZE,

    /** 建议重新录制。 */
    RETRY,
}
