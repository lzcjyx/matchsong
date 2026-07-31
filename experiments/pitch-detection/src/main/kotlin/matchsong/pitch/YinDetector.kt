package matchsong.pitch

import kotlin.math.abs
import kotlin.math.max

/**
 * YIN 音高检测算法（Spike 用途，纯 Kotlin 实现）。
 *
 * 参考：de Cheveigné & Kawahara, "YIN, a fundamental frequency estimator
 * for speech and music", JASA 111(4), 2002.（来源见 docs/research/source-register.md）
 *
 * 步骤：
 * 1. 差分函数 d[tau] = sum (x[j] - x[j+tau])^2
 * 2. 累积均值归一差分函数 d'[tau] = d[tau] / ((1/tau) * sum_{j<tau} d[j])
 * 3. 绝对阈值：找第一个低于 threshold (0.10) 的谷，取该谷的局部极小
 * 4. 抛物线插值精化
 * 5. 若无低于阈值的谷，取全局最小（probability 较低）
 */
object YinDetector {

    private const val THRESHOLD = 0.10

    fun detect(
        frame: DoubleArray,
        sampleRate: Int,
        minFreq: Double,
        maxFreq: Double,
    ): Pitch.PitchResult {
        val tauMin = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
        val tauMax = (sampleRate / minFreq).toInt().coerceAtMost(frame.size / 2)

        // 步骤 1：差分函数
        val diff = DoubleArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var sum = 0.0
            var j = 0
            while (j + tau < frame.size) {
                val d = frame[j] - frame[j + tau]
                sum += d * d
                j++
            }
            diff[tau] = sum
        }

        // 步骤 2：累积均值归一
        val cmnd = DoubleArray(tauMax + 1)
        cmnd[0] = 1.0
        var running = 0.0
        for (tau in 1..tauMax) {
            running += diff[tau]
            cmnd[tau] = if (running > 0) diff[tau] * tau / running else 1.0
        }

        // 步骤 3：绝对阈值
        var tau = tauMin
        while (tau < tauMax) {
            if (cmnd[tau] < THRESHOLD) {
                // 下降到阈值以下后，继续走到该谷的局部极小
                var t = tau
                while (t + 1 < tauMax && cmnd[t + 1] < cmnd[t]) t++
                val refined = parabolicInterp(cmnd, t)
                val f0 = sampleRate / refined
                val prob = 1.0 - cmnd[t].coerceIn(0.0, 1.0)
                return Pitch.PitchResult(f0, prob, "YIN", Pitch.rms(frame))
            }
            tau++
        }

        // 步骤 5：无低于阈值的谷，取全局最小作为低可信度候选
        var minTau = tauMin
        var minVal = Double.MAX_VALUE
        for (t in tauMin until tauMax) {
            if (cmnd[t] < minVal) {
                minVal = cmnd[t]
                minTau = t
            }
        }
        // 若全局最小仍很高（>0.5），判定无音高
        return if (minVal > 0.5) {
            Pitch.noPitch("YIN", Pitch.rms(frame))
        } else {
            val refined = parabolicInterp(cmnd, minTau)
            val f0 = sampleRate / refined
            val prob = 1.0 - minVal.coerceIn(0.0, 1.0)
            Pitch.PitchResult(f0, prob, "YIN", Pitch.rms(frame))
        }
    }

    /** 抛物线插值精化 tau。 */
    private fun parabolicInterp(cmnd: DoubleArray, tau: Int): Double {
        if (tau <= 0 || tau >= cmnd.size - 1) return tau.toDouble()
        val s0 = cmnd[tau - 1]
        val s1 = cmnd[tau]
        val s2 = cmnd[tau + 1]
        val denom = 2.0 * (2 * s1 - s2 - s0)
        if (abs(denom) < 1e-9) return tau.toDouble()
        val delta = (s2 - s0) / denom
        return (tau + delta).coerceIn((tau - 1).toDouble(), (tau + 1).toDouble())
    }
}
