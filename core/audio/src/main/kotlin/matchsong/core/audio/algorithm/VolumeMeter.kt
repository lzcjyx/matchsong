package matchsong.core.audio.algorithm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import matchsong.core.audio.api.AudioChunk
import matchsong.domain.recording.VolumeLevel
import kotlin.math.abs

/**
 * M3.6-1 实时音量计算器（FR-REC-4，ARCHITECTURE.md §14.2）。
 *
 * 输入一帧 [AudioChunk]（RMS/峰值已在采集侧算好），输出 UI 消费的 [VolumeLevel]：
 * - [VolumeLevel.isTooQuiet]：rms < Q-2 低音量阈值；
 * - [VolumeLevel.isClipping]：帧内连续满幅样本 ≥ Q-3 阈值（与 M4.2 质量检测同一判定标准）；
 * - [VolumeLevel.hasInput]：rms > Q-1 静音阈值。
 *
 * 阈值来自 [QualityThresholds] 集中配置（data-model §5.1），构造时注入、可覆盖。
 */
class VolumeMeter(
    private val thresholds: QualityThresholds = QualityThresholds.DEFAULTS,
) {
    /** 计算一帧 [chunk] 的音量级。纯函数：无副作用、无 IO。 */
    fun computeVolume(chunk: AudioChunk): VolumeLevel {
        val rms = chunk.rms
        return VolumeLevel(
            rms = rms,
            isTooQuiet = rms < thresholds.quietRmsThreshold,
            isClipping = hasClippingRun(chunk.samples),
            hasInput = rms > thresholds.silenceRmsThreshold,
        )
    }

    /**
     * Q-3 削波判定：帧内是否存在 ≥ [QualityThresholds.clippingConsecutiveFullScaleSamples]
     * 个连续满幅样本（|sample| ≥ [QualityThresholds.clippingFullScaleMagnitude]）。
     *
     * §5.1 Q-3 的「削波帧比例 > 0.05」为整段录音级指标，由 M4.2 质量分析按帧统计，不在此逐帧判定。
     */
    private fun hasClippingRun(samples: FloatArray): Boolean {
        var consecutiveFullScale = 0
        for (sample in samples) {
            consecutiveFullScale =
                if (abs(sample) >= thresholds.clippingFullScaleMagnitude) {
                    consecutiveFullScale + 1
                } else {
                    0
                }
            if (consecutiveFullScale >= thresholds.clippingConsecutiveFullScaleSamples) {
                return true
            }
        }
        return false
    }
}

/**
 * 音量流节流操作符（FR-REC-4，≤10Hz；ARCHITECTURE.md §14.2）。
 *
 * 委托通用 [throttleLatest]：默认每 100ms 最多发射一次，保留最新值。
 */
fun Flow<VolumeLevel>.throttledVolume(periodMs: Long = 100): Flow<VolumeLevel> = throttleLatest(periodMs)

/**
 * 通用节流操作符：最多每 [periodMs] 毫秒向下游发射一次。
 *
 * 语义（conflate + 时间窗）：
 * - 首个值立即发射，保证 UI 首帧即有反馈；
 * - 窗口内的中间值被丢弃（conflate），只保留最新；
 * - 窗口结束时若窗口内有新值到达则补发最新值——上游完成时最后一个值也保证送达（不丢尾）；
 * - 上游两次发射间隔 ≥ [periodMs] 时原样通过。
 *
 * @throws IllegalArgumentException [periodMs] 非正时。
 */
fun <T> Flow<T>.throttleLatest(periodMs: Long): Flow<T> {
    require(periodMs > 0) { "periodMs 必须为正，当前值：$periodMs" }
    return flow {
        var latest: T? = null
        var hasNewValue = false
        this@throttleLatest.conflate().collect { value ->
            latest = value
            hasNewValue = true
            // 发射后进入静默窗口；窗口内到达的值只保留最新（conflate），窗口结束补发
            do {
                emit(latest as T)
                hasNewValue = false
                delay(periodMs)
            } while (hasNewValue)
        }
    }
}
