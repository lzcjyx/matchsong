package matchsong.domain.analysis

/**
 * M5.6-1 完整分析结果（FR-ANAL-6，data-model §2.7）。
 */
data class VoiceAnalysisResult(
    /** 质量报告摘要（isUsable 等，M4 输出）。 */
    val qualityUsable: Boolean,
    val qualityWarnings: List<String>,
    /** 稳定音域估计（样本不足时为 null，FR-ANAL-8）。 */
    val vocalRange: VocalRangeEstimate?,
    /** 舒适音区估计（样本不足时为 null）。 */
    val comfortRange: ComfortRangeEstimate?,
    /** 音高稳定性指标。 */
    val stability: PitchStabilityMetricsResult?,
    /** 有效帧数。 */
    val voicedFrameCount: Int,
    /** 总帧数。 */
    val totalFrameCount: Int,
    /** 置信度分档（SPEC §13：HIGH ≥0.7 / MEDIUM [0.5,0.7) / LOW <0.5）。 */
    val confidenceLevel: ConfidenceLevel,
    /** 警告（INSUFFICIENT_SAMPLES 等）。 */
    val warnings: List<AnalysisWarning>,
    /** 算法版本（FR-HX-1，随结果落库）。 */
    val algorithmVersion: String,
)

/**
 * 置信度分档（SPEC §13）。
 */
enum class ConfidenceLevel { HIGH, MEDIUM, LOW }
