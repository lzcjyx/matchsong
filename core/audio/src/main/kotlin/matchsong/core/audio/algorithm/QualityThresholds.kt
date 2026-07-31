package matchsong.core.audio.algorithm

/**
 * M3.6-1 音量/质量阈值集中配置（data-model §5.1 常量表 Q-1 ~ Q-3）。
 *
 * 阈值集中定义于此（§5.1「阈值集中配置原则」），禁止散落代码；
 * 默认值与 §5.1 一致：Q-1 为实测值，Q-2/Q-3 的浮点细节为 [推测] 保守默认，待 M4.2 实测标定。
 *
 * @property silenceRmsThreshold Q-1 静音 RMS 阈值（归一化幅值）：rms 不大于该值视为静音/无输入（实测 0.01）。
 * @property quietRmsThreshold Q-2 低音量 RMS 阈值（归一化幅值）：rms 低于该值视为音量过低（[推测] 0.02，须 > Q-1）。
 * @property clippingFullScaleMagnitude Q-3 满幅样本幅值下限（归一化）：|sample| 不低于该值视为满幅样本。
 *   [推测] 归一化浮点下的保守取值；PCM16 满幅为 32767/32768 ≈ 0.99997。
 * @property clippingConsecutiveFullScaleSamples Q-3 削波判定：一帧内连续满幅样本数达到该值即判为削波（≥ 3）。
 */
data class QualityThresholds(
    val silenceRmsThreshold: Double = 0.01,
    val quietRmsThreshold: Double = 0.02,
    val clippingFullScaleMagnitude: Float = 0.999f,
    val clippingConsecutiveFullScaleSamples: Int = 3,
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
    }

    companion object {
        /** data-model §5.1 默认值（Q-1 实测；Q-2/Q-3 为 [推测] 保守默认，M4.2 标定）。 */
        val DEFAULTS = QualityThresholds()
    }
}
