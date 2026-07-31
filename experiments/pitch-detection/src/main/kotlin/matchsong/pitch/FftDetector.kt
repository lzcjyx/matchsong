package matchsong.pitch

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 基于 FFT 的音高检测（Spike 用途）。
 *
 * 流程：
 * 1. Hann 窗
 * 2. 实数 FFT（朴素 Cooley-Tukey，要求帧长为 2 的幂）
 * 3. 取幅度谱，在 [minFreq, maxFreq] 内找主峰
 * 4. 抛物线插值精化频率
 *
 * 局限：频谱主峰对应的是最强谐波，不一定是基频（尤其对谐波丰富/基频弱的人声）。
 */
object FftDetector {

    fun detect(
        frame: DoubleArray,
        sampleRate: Int,
        minFreq: Double,
        maxFreq: Double,
    ): Pitch.PitchResult {
        require(frame.size and (frame.size - 1) == 0) { "FFT 要求帧长为 2 的幂，当前=${frame.size}" }

        // Hann 窗
        val windowed = DoubleArray(frame.size) { i ->
            val w = 0.5 * (1 - cos(2 * PI * i / (frame.size - 1)))
            frame[i] * w
        }

        // 实数 FFT：构造复数数组（实部=信号，虚部=0）
        val re = windowed.copyOf()
        val im = DoubleArray(frame.size)
        fft(re, im)

        // 单边幅度谱
        val half = frame.size / 2
        val mag = DoubleArray(half)
        for (k in 0 until half) {
            mag[k] = sqrt(re[k] * re[k] + im[k] * im[k])
        }
        val binHz = sampleRate.toDouble() / frame.size
        val kMin = (minFreq / binHz).toInt().coerceAtLeast(1)
        val kMax = (maxFreq / binHz).toInt().coerceAtMost(half - 1)

        var peakK = kMin
        var peakMag = 0.0
        for (k in kMin..kMax) {
            if (mag[k] > peakMag) {
                peakMag = mag[k]
                peakK = k
            }
        }
        if (peakMag <= 0.0) return Pitch.noPitch("FFT", Pitch.rms(frame))

        // 抛物线插值精化频率 bin
        val refined = parabolicInterp(mag, peakK)
        val f0 = refined * binHz
        // 可信度：主峰与谱均值之比
        val mean = mag.sliceArray(kMin..kMax).average()
        val prob = (peakMag / (mean + 1e-9) / 10.0).coerceIn(0.0, 1.0)
        return Pitch.PitchResult(f0, prob, "FFT", Pitch.rms(frame))
    }

    /** 迭代 Cooley-Tukey radix-2 FFT（原地）。n 必须为 2 的幂。 */
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        // 位反转
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j and bit.inv()
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        // 蝶形
        var len = 2
        while (len <= n) {
            val ang = -2 * PI / len
            val wre = cos(ang)
            val wim = sin(ang)
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                val half = len / 2
                for (k in 0 until half) {
                    val a = i + k
                    val b = i + k + half
                    val tRe = curRe * re[b] - curIm * im[b]
                    val tIm = curRe * im[b] + curIm * re[b]
                    re[b] = re[a] - tRe
                    im[b] = im[a] - tIm
                    re[a] = re[a] + tRe
                    im[a] = im[a] + tIm
                    val nRe = curRe * wre - curIm * wim
                    val nIm = curRe * wim + curIm * wre
                    curRe = nRe
                    curIm = nIm
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun parabolicInterp(mag: DoubleArray, k: Int): Double {
        if (k <= 0 || k >= mag.size - 1) return k.toDouble()
        val y0 = ln(mag[k - 1] + 1e-12)
        val y1 = ln(mag[k] + 1e-12)
        val y2 = ln(mag[k + 1] + 1e-12)
        val denom = 2.0 * (2 * y1 - y2 - y0)
        if (abs(denom) < 1e-9) return k.toDouble()
        val delta = (y2 - y0) / denom
        return (k + delta).coerceIn((k - 1).toDouble(), (k + 1).toDouble())
    }

    @Suppress("unused") private fun log2d(x: Double): Double = ln(x) / ln(2.0)
}
