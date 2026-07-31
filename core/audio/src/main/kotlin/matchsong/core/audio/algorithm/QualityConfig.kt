package matchsong.core.audio.algorithm

/**
 * M4.2-1 质量检测阈值集中配置（FR-QUAL-2，data-model §5.1 常量表 Q-1 ~ Q-5）。
 *
 * 注意：M4.2-1 原计划将 QualityConfig 置于 core:model，但 M3.6-1 已在
 * core:audio.algorithm 建立 QualityThresholds（VolumeMeter 依赖），此处扩展为
 * 完整 QualityConfig 并保留原位置，避免移动依赖（记录为 M0.3/M3.6 偏差）。
 *
 * 阈值集中定义于此，禁止散落代码；默认值与 §5.1 一致：
 * Q-1 为实测值，Q-2/Q-3/Q-4/Q-5 为 [推测] 保守默认，M4.6-2 夹具实测标定。
 */
data class QualityConfig(
    /** Q-1 静音 RMS 阈值（归一化幅值）：rms ≤ 该值视为静音/无输入（实测 0.01）。 */
    val silenceRmsThreshold: Double = 0.01,
    /** Q-2 低音量 RMS 阈值：rms < 该值视为音量过低（[推测] 0.02，须 > Q-1）。 */
    val quietRmsThreshold: Double = 0.02,
    /** Q-3 满幅样本幅值下限（归一化）：|sample| ≥ 该值视为满幅样本（[推测] 0.999）。 */
    val clippingFullScaleMagnitude: Float = 0.999f,
    /** Q-3 削波判定：一帧内连续满幅样本数 ≥ 该值即判为削波帧（≥ 3）。 */
    val clippingConsecutiveFullScaleSamples: Int = 3,
    /** Q-4 严重削波：削波帧比例 ≥ 该值（[推测] 0.05）。 */
    val clippingRatioLimit: Double = 0.05,
    /** Q-4 最小有效声音时长（毫秒）：有效声音累计 < 该值视为有效片段不足（[推测] 5000ms）。 */
    val minActiveVoiceDurationMs: Long = 5_000,
    /** Q-5 最小有效帧比例：有效帧/总帧 < 该值视为有效片段不足（[推测] 0.30）。 */
    val minActiveFrameRatio: Double = 0.30,
    /** R-3 最短录音时长（毫秒）：录音 < 该值视为过短（SPEC §6 约 10s）。 */
    val minDurationMs: Long = 10_000,
) {
    init {
        require(quietRmsThreshold > silenceRmsThreshold) {
            "Q-2 低音量阈值（$quietRmsThreshold）必须大于 Q-1 静音阈值（$silenceRmsThreshold）"
        }
        require(clippingConsecutiveFullScaleSamples > 0) {
            "Q-3 连续满幅样本数必须为正，当前值：$clippingConsecutiveFullScaleSamples"
        }
        require(clippingFullScaleMagnitude > 0f && clippingFullScaleMagnitude <= 1f) {
            "Q-3 满幅幅值必须在 (0, 1] 内，当前值：$clippingFullScaleMagnitude"
        }
        require(minActiveFrameRatio > 0.0 && minActiveFrameRatio <= 1.0) {
            "Q-5 有效帧比例必须在 (0, 1] 内，当前值：$minActiveFrameRatio"
        }
    }

    companion object {
        /** data-model §5.1 默认值（Q-1 实测；其余为 [推测] 保守默认，M4.6-2 标定）。 */
        val DEFAULTS = QualityConfig()
    }
}

/**
 * 兼容别名：M3.6-1 的 VolumeMeter 依赖 QualityThresholds；
 * 扩展后统一为 [QualityConfig]，别名保证旧引用不破坏（M4 后删除）。
 */
@Deprecated("M4.2-1 起使用 QualityConfig（统一阈值配置）")
typealias QualityThresholds = QualityConfig
