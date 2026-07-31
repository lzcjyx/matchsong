package matchsong.pitch

import kotlin.math.abs

/**
 * M-1.5 音高检测 Spike 评估入口。
 *
 * 对每种合成信号 × 每种算法，分帧检测并统计：
 * - 中位检出频率 / 对应 MIDI
 * - 检出率（有效帧占比）
 * - 平均相对误差（对有真值的信号）
 * - 平均每帧处理时间
 *
 * 输出纯文本表格到 stdout，供 docs/experiments/pitch-detection-results.md 引用。
 *
 * 注意：本评估基于合成信号，不等于真实人声表现（见文档“局限”章节）。
 */
object PitchSpikeEval {

    data class Case(
        val name: String,
        val signal: DoubleArray,
        val trueFreq: Double, // Hz，NaN 表示无明确真值（噪声/说话/削波）
    )

    data class Row(
        val case: String,
        val method: String,
        val medianFreq: Double,
        val medianMidi: Double,
        val detectionRate: Double,
        val meanRelError: Double,
        val meanFrameMs: Double,
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val sr = Signals.SAMPLE_RATE
        val cases = listOf(
            Case("sine_220_A3", Signals.sine(220.0, 2.0), 220.0),
            Case("sine_440_A4", Signals.sine(440.0, 2.0), 440.0),
            Case("sine_880_A5", Signals.sine(880.0, 2.0), 880.0),
            Case("sine_130_C3_maleLow", Signals.sine(130.0, 2.0), 130.0),
            Case("sine_1046_C6_femaleHigh", Signals.sine(1046.0, 2.0), 1046.0),
            Case("scale_C3_E3_G3_C4", Signals.scale(intArrayOf(48, 52, 55, 60), 0.5), Double.NaN),
            Case("silence", Signals.silence(2.0), Double.NaN),
            Case("whiteNoise", Signals.whiteNoise(2.0), Double.NaN),
            Case("clipped_440", Signals.clipped(440.0, 2.0), 440.0),
            Case("talkLike_150", Signals.talkLike(2.0, 150.0), 150.0),
        )
        val methods = listOf("YIN", "ACF", "FFT")
        val frameSize = 2048
        val hop = 1024

        val rows = ArrayList<Row>()
        for (c in cases) {
            for (m in methods) {
                rows.add(evaluate(c, m, frameSize, hop, sr))
            }
        }
        printTable(rows)
    }

    private fun evaluate(c: Case, method: String, frameSize: Int, hop: Int, sr: Int): Row {
        val frames = Pitch.detectFrames(c.signal, method, frameSize, hop, sr)
        val times = ArrayList<Double>()
        val freqs = ArrayList<Double>()
        for (f in frames) {
            times.add(0.0) // 占位，测时单独做
            if (!f.f0Hz.isNaN() && f.f0Hz in 20.0..2000.0) freqs.add(f.f0Hz)
        }
        // 真实测时：单独跑 50 帧取平均
        val timingNs = ArrayList<Long>()
        val nTiming = 50
        var i = 0
        var cnt = 0
        while (i + frameSize <= c.signal.size && cnt < nTiming) {
            val frame = c.signal.copyOfRange(i, i + frameSize)
            val t0 = System.nanoTime()
            Pitch.detect(frame, method, sr)
            timingNs.add(System.nanoTime() - t0)
            i += hop
            cnt++
        }
        val meanMs = (timingNs.average() / 1e6)

        val medianFreq = if (freqs.isEmpty()) Double.NaN else median(freqs)
        val medianMidi = if (medianFreq.isNaN()) Double.NaN else Notes.freqToMidi(medianFreq)
        val detRate = freqs.size.toDouble() / frames.size
        val relErr = if (c.trueFreq.isNaN() || freqs.isEmpty()) Double.NaN else {
            freqs.map { abs(it - c.trueFreq) / c.trueFreq }.average()
        }
        return Row(c.name, method, medianFreq, medianMidi, detRate, relErr, meanMs)
    }

    private fun median(xs: List<Double>): Double {
        val s = xs.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }

    private fun printTable(rows: List<Row>) {
        println("=== M-1.5 Pitch Detection Spike Results ===")
        println("Frame=2048, Hop=1024, SR=44100, JDK=17, Kotlin=2.1.0, pure Kotlin")
        println()
        printf("%-24s %-5s %-10s %-10s %-10s %-10s %-10s%n", "case", "method", "medF(Hz)", "medMIDI", "detRate", "relErr", "ms/frame")
        for (r in rows) {
            printf(
                "%-24s %-5s %-10s %-10s %-10.3f %-10s %-10.3f%n",
                r.case, r.method,
                fmt(r.medianFreq), fmt(r.medianMidi),
                r.detectionRate,
                if (r.meanRelError.isNaN()) "n/a" else "%.5f".format(r.meanRelError),
                r.meanFrameMs,
            )
        }
    }

    private fun fmt(v: Double): String = if (v.isNaN()) "NaN" else "%.2f".format(v)
    private fun printf(fmt: String, vararg args: Any?) = System.out.printf(fmt, *args)
}
