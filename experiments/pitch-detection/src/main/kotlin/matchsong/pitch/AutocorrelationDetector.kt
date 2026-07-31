package matchsong.pitch

import kotlin.math.max
import kotlin.math.min

/**
 * 朴素自相关音高检测（Spike 用途）。
 *
 * 流程：
 * 1. 对帧做归一化自相关 r[tau] = sum x[i]*x[i+tau] / sqrt(...)
 * 2. 在 [tauMin, tauMax] 内找最大峰
 * 3. f0 = sampleRate / tau
 *
 * 局限：对谐波丰富的信号可能锁定到倍频或分频，鲁棒性弱于 YIN。
 */
object AutocorrelationDetector {

    fun detect(
        frame: DoubleArray,
        sampleRate: Int,
        minFreq: Double,
        maxFreq: Double,
    ): Pitch.PitchResult {
        val tauMin = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
        val tauMax = (sampleRate / minFreq).toInt().coerceAtMost(frame.size - 1)

        var bestTau = 0
        var bestVal = -Double.MAX_VALUE
        // 归一化自相关
        for (tau in tauMin..tauMax) {
            var sum = 0.0
            var norm = 0.0
            var i = 0
            while (i + tau < frame.size) {
                sum += frame[i] * frame[i + tau]
                norm += frame[i] * frame[i]
                i++
            }
            // 简化归一：用首样本能量近似
            if (norm > 0) {
                val r = sum / norm
                if (r > bestVal) {
                    bestVal = r
                    bestTau = tau
                }
            }
        }
        if (bestTau <= 0 || bestVal < 0.0) {
            return Pitch.noPitch("ACF", Pitch.rms(frame))
        }
        val f0 = sampleRate.toDouble() / bestTau
        val prob = bestVal.coerceIn(0.0, 1.0)
        return Pitch.PitchResult(f0, prob, "ACF", Pitch.rms(frame))
    }
}
