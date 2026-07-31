package matchsong.domain.analysis

/**
 * M5.3-5 分析配置常量（data-model §1.4 阈值集中配置 + §5.1 默认值表）。
 *
 * [推测] 标注：数值为保守默认，M5.8 真机数据标定后随版本记录（data-model §5.2）。
 * 说明：data-model §5.1 要求集中至 core:model 配置对象；当前阶段配置先行落在
 * domain:analysis（domain 仅能依赖 core:model，而 core:model 尚无分析配置），
 * M5.6 组装 VoiceAnalysisResult 时迁移/对齐。
 */
object AnalysisConfig {
    // ---------- A 系列：音高分析（ADR-003） ----------

    /** A-1 分析频率下限（MIDI 36 = C2）。 */
    const val MIN_MIDI = 36.0

    /** A-2 分析频率上限（MIDI 84 = C6）。 */
    const val MAX_MIDI = 84.0

    /** A-4 音域低分位数（P5，PLAN M5.3，可配置）。 */
    const val LOW_QUANTILE = 0.05

    /** A-4 音域高分位数（P95，且 > LOW_QUANTILE）。 */
    const val HIGH_QUANTILE = 0.95

    /** A-5 有效帧充足阈值（120 帧 ≈ 5.5s 有效演唱）[推测]。 */
    const val MIN_VOICED_FRAMES = 120

    /** A-3 帧周期（毫秒，2048/1024 @44.1kHz ≈ 46ms/帧，用于时长换算）。 */
    const val FRAME_PERIOD_MS = 46L

    /** A-6 低置信度阈值（confidence < 0.5 → LOW，SPEC §13）。 */
    const val LOW_CONFIDENCE_THRESHOLD = 0.5

    // ---------- M5.3-2 置信度公式 [推测]（M5.8 标定） ----------

    /** 帧数因子饱和帧数（= 2×A-5：240 帧以上不再提升）。 */
    const val CONFIDENCE_COUNT_SATURATION_FRAMES: Double = 2.0 * MIN_VOICED_FRAMES

    /** 分布紧凑度半衰期（半音：IQR=12 → 紧凑度 0.5）。 */
    const val CONFIDENCE_COMPACTNESS_HALF_LIFE_SEMITONES = 12.0

    // ---------- M5.5 音高稳定性 [推测]（M5.8 标定） ----------

    /** 稳定判定窗（音分，±50）。 */
    const val STABLE_CENTS_TOLERANCE = 50.0

    /** 稳定片段最少帧数（≥ 2：孤立单帧不算稳定片段）。 */
    const val MIN_STABLE_RUN_FRAMES = 2

    /** 长音时长阈值（毫秒，≥ 该时长计入长音波动）。 */
    const val LONG_NOTE_DURATION_MS = 800L

    // ---------- M5.4 舒适音区 [推测]（M5.8 真机标定） ----------

    /** 同音连续片段判定容差（半音 = ±50 音分）。 */
    const val COMFORT_SAME_NOTE_TOLERANCE_SEMITONES: Double = STABLE_CENTS_TOLERANCE / 100.0

    /** 边缘样本最少权重（低于该得分的半音桶不纳入舒适区，防止稀疏边缘拉宽音区）。 */
    const val COMFORT_EDGE_MIN_FRAMES = 10.0

    /** 主要演唱音区扩展比例（相邻桶得分 ≥ 峰值×该比例才纳入主音区）。 */
    const val COMFORT_PRIMARY_PEAK_RATIO = 0.5

    /** M5.4 "本次录音估计"声明（FR-ANAL-4/7，语义固定）。 */
    const val ESTIMATE_DISCLAIMER = "本次录音估计"
}
