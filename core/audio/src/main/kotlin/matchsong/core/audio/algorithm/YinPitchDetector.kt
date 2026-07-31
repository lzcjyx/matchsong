package matchsong.core.audio.algorithm

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import matchsong.core.audio.api.PitchFrame
import matchsong.core.audio.api.PitchTrack
import matchsong.core.audio.api.PitchTracker
import matchsong.core.audio.api.PitchTrackerConfig
import kotlin.math.abs

/**
 * M5.1-1 YIN 音高检测生产实现（ADR-003）。
 *
 * 参考：de Cheveigné & Kawahara, "YIN, a fundamental frequency estimator
 * for speech and music", JASA 111(4), 2002（来源见 docs/research/source-register.md）。
 *
 * M-1.5 Spike 实测基线（合成信号）：正弦 130-1046Hz 误差 <0.03%；
 * 静音/白噪声正确拒绝；~1.04ms/帧（桌面 JVM）。
 *
 * 实现为对 Spike 的重构（PLAN §3.2：实验代码不得直接复制进生产）：
 * - 配置对象注入（PitchTrackerConfig）；
 * - 输入 [Frame]（M4.1-1 帧管线，与质量检测共用）；
 * - 批量 API suspend + 取消检查（M5.1-2）。
 */
class YinPitchDetector(
    private val config: PitchTrackerConfig = PitchTrackerConfig(),
) : PitchTracker {
    override fun detectFrame(frame: Frame): PitchFrame {
        val rms = frame.stats.rms
        // 静音门限（与 QualityConfig Q-1 一致）
        if (rms < config.silenceRmsThreshold) {
            return PitchFrame(
                timestampMs = frame.timestampMs,
                f0Hz = Double.NaN,
                midiNote = Double.NaN,
                confidence = 0.0,
                rms = rms,
                isVoiced = false,
            )
        }

        val result = yinDetect(frame.samples, config)
        return when {
            result == null -> PitchFrame(frame.timestampMs, Double.NaN, Double.NaN, 0.0, rms, false)
            // 频率越界 → 无效帧；边界 ±2% 容差（1046Hz 检出 1050Hz 属 YIN 边界精度，M5.8 实测）
            result.f0 < config.minFreqHz * 0.98 || result.f0 > config.maxFreqHz * 1.02 -> {
                PitchFrame(frame.timestampMs, Double.NaN, Double.NaN, 0.0, rms, false)
            }
            else -> {
                val midi = 69.0 + 12.0 * kotlin.math.ln(result.f0 / 440.0) / kotlin.math.ln(2.0)
                PitchFrame(
                    timestampMs = frame.timestampMs,
                    f0Hz = result.f0,
                    midiNote = midi,
                    confidence = result.confidence,
                    rms = rms,
                    isVoiced = true,
                )
            }
        }
    }

    override suspend fun track(frames: List<Frame>): PitchTrack {
        val out = ArrayList<PitchFrame>(frames.size)
        for (f in frames) {
            currentCoroutineContext().ensureActive() // M5.1-2 取消检查
            out.add(detectFrame(f))
        }
        return PitchTrack(
            frames = out,
            voicedFrameCount = out.count { it.isVoiced },
            algorithmVersion = ALGORITHM_VERSION,
        )
    }

    private data class YinResult(val f0: Double, val confidence: Double)

    private fun yinDetect(
        frame: FloatArray,
        config: PitchTrackerConfig,
    ): YinResult? {
        // 高频帧（如 1046Hz）在 float 精度下 CMND 计算会失效（M5.8 实测），
        // 转 Double 精度计算（与 Spike 的 DoubleArray 基线一致）
        val raw = DoubleArray(frame.size) { frame[it].toDouble() }
        // 一阶高通预滤波（alpha=0.95，截止约 350Hz）：去除低频伴奏/底噪，
        // 显著改善真实人声基频谷深度（M5.8 MIR-1K 实测：男声 73.6→186.9Hz，女声 82.7→286.4Hz）
        val dFrame = highpass(raw)
        val sampleRate = config.sampleRateHz
        val tauMin = (sampleRate / config.maxFreqHz).toInt().coerceAtLeast(2)
        val tauMax = (sampleRate / config.minFreqHz).toInt().coerceAtMost(frame.size / 2)
        if (tauMax <= tauMin) return null

        // 步骤 1：差分函数 d[tau] = sum (x[j] - x[j+tau])^2
        val diff = DoubleArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var sum = 0.0
            var j = 0
            while (j + tau < dFrame.size) {
                val d = dFrame[j] - dFrame[j + tau]
                sum += d * d
                j++
            }
            diff[tau] = sum
        }

        // 步骤 2：累积均值归一化差分函数（CMND）
        val cmnd = DoubleArray(tauMax + 1)
        cmnd[0] = 1.0
        var running = 0.0
        for (tau in 1..tauMax) {
            running += diff[tau]
            cmnd[tau] = if (running > 0) diff[tau] * tau / running else 1.0
        }

        // 步骤 3：绝对阈值找第一个谷
        var tau = tauMin
        while (tau < tauMax) {
            if (cmnd[tau] < config.threshold) {
                var t = tau
                while (t + 1 < tauMax && cmnd[t + 1] < cmnd[t]) t++
                val refined = parabolicInterp(cmnd, t)
                val f0 = sampleRate / refined
                val prob = 1.0 - cmnd[t].coerceIn(0.0, 1.0)
                return YinResult(f0, prob)
            }
            tau++
        }

        // 步骤 4：无低于阈值的谷 → 全局最小（低可信度候选或判无音）
        var minTau = tauMin
        var minVal = Double.MAX_VALUE
        for (t in tauMin until tauMax) {
            if (cmnd[t] < minVal) {
                minVal = cmnd[t]
                minTau = t
            }
        }
        if (minVal > config.noPitchThreshold) return null
        val refined = parabolicInterp(cmnd, minTau)
        val f0 = sampleRate / refined
        val prob = 1.0 - minVal.coerceIn(0.0, 1.0)
        return YinResult(f0, prob)
    }

    /** 一阶高通滤波（alpha=0.95）：y[i] = alpha*(y[i-1] + x[i] - x[i-1])。 */
    private fun highpass(x: DoubleArray): DoubleArray {
        val y = DoubleArray(x.size)
        for (i in 1 until x.size) {
            y[i] = HIGHPASS_ALPHA * (y[i - 1] + x[i] - x[i - 1])
        }
        return y
    }

    /** 抛物线插值精化 tau。 */
    private fun parabolicInterp(
        cmnd: DoubleArray,
        tau: Int,
    ): Double {
        if (tau <= 0 || tau >= cmnd.size - 1) return tau.toDouble()
        val s0 = cmnd[tau - 1]
        val s1 = cmnd[tau]
        val s2 = cmnd[tau + 1]
        val denom = 2.0 * (2 * s1 - s2 - s0)
        if (abs(denom) < 1e-9) return tau.toDouble()
        val delta = (s2 - s0) / denom
        return (tau + delta).coerceIn((tau - 1).toDouble(), (tau + 1).toDouble())
    }

    companion object {
        const val ALGORITHM_VERSION = "1.0.0"

        /** 高通系数（截止约 350Hz@44.1k；M5.8 人声标定）。 */
        const val HIGHPASS_ALPHA = 0.95
    }
}
