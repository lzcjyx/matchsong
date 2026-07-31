package matchsong.pitch

/**
 * 三种音高检测算法的纯 Kotlin 实现（Spike 用途，不进生产模块）。
 *
 * - [AutocorrelationDetector]：朴素自相关 + 峰值拾取。
 * - [YinDetector]：de Cheveigné & Kawahara 2002 的 YIN 算法。
 * - [FftDetector]：基于 FFT 的频谱主峰检测。
 *
 * 所有 detector 返回 [PitchResult]；检测不到稳定音高时返回 f0 = NaN。
 */
object Pitch {

    /** 单帧检测结果。 */
    data class PitchResult(
        val f0Hz: Double,
        val probability: Double, // 0..1，算法给出的可信度（YIN 为 1 - periodicity）
        val method: String,
        val frameRms: Double,
    )

    /** 无音高结果。 */
    fun noPitch(method: String, frameRms: Double): PitchResult =
        PitchResult(Double.NaN, 0.0, method, frameRms)

    private const val MIN_FREQ = 65.0   // 约男低音低限 C2
    private const val MAX_FREQ = 1100.0 // 约女高音高限 (~C#6)

    /** 对单帧执行检测。frame 为一帧 PCM double[-1,1]。 */
    fun detect(frame: DoubleArray, method: String, sampleRate: Int = Signals.SAMPLE_RATE): PitchResult {
        val rms = rms(frame)
        // 静音门限：RMS < 0.01 视为无音
        if (rms < 0.01) return noPitch(method, rms)
        return when (method) {
            "YIN" -> YinDetector.detect(frame, sampleRate, MIN_FREQ, MAX_FREQ)
            "ACF" -> AutocorrelationDetector.detect(frame, sampleRate, MIN_FREQ, MAX_FREQ)
            "FFT" -> FftDetector.detect(frame, sampleRate, MIN_FREQ, MAX_FREQ)
            else -> throw IllegalArgumentException("unknown method: $method")
        }
    }

    /** RMS（均方根）。 */
    fun rms(frame: DoubleArray): Double {
        var sum = 0.0
        for (v in frame) sum += v * v
        return Math.sqrt(sum / frame.size)
    }

    /**
     * 分帧检测整段信号，返回逐帧结果。
     * frameSize 为帧长样本数，hopSize 为步进。
     */
    fun detectFrames(
        signal: DoubleArray,
        method: String,
        frameSize: Int = 2048,
        hopSize: Int = 1024,
        sampleRate: Int = Signals.SAMPLE_RATE,
    ): List<PitchResult> {
        val out = ArrayList<PitchResult>()
        var i = 0
        while (i + frameSize <= signal.size) {
            val frame = signal.copyOfRange(i, i + frameSize)
            out.add(detect(frame, method, sampleRate))
            i += hopSize
        }
        return out
    }
}
