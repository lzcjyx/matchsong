package matchsong.domain.analysis

import kotlin.math.abs

/**
 * M5.5-1 音高稳定性指标（FR-ANAL-5，PLAN M5.5，data-model §2.7）。
 *
 * 输出四项指标（**不输出"唱功分数"**，P7——指标仅供 M7 的
 * DifficultyFit/PitchStabilityFit 消费）：
 * - [stableFrameRatio] [推测]：稳定帧比例。第一遍按"相邻帧差 ≤ ±50 音分"切分
 *   连续片段；第二遍剔除相对片段中位数偏差 > ±50 音分的帧（忠实于"局部中位数
 *   ±50 音分"定义，剔除片段内的漂移尾帧）；剩余长度 ≥ 2 帧的片段为稳定片段，
 *   stableFrameRatio = 稳定片段帧数 / 总有效帧数；
 * - [pitchDeviationCents]：有效帧音分（midi×100）相对整体中位数的中位绝对偏差（MAD）；
 * - [longNoteDeviationCents]：时长 ≥ LONG_NOTE_DURATION_MS（800ms [推测]）的
 *   长音片段内，帧相对片段中位数的平均绝对偏差（逐帧 F0 抖动）；
 * - [voicedFrameRatio]：有效帧 / 总帧（传入后处理轨迹时恒为 1.0——所有帧均已
 *   通过有效+置信度过滤；原始比率需传原始轨迹 [推测]）。
 *
 * 纯 Kotlin 对象，零 Android 依赖。
 */
object PitchStabilityMetrics {
    /** 算法版本（语义化，data-model §5.2）。 */
    const val METHOD_VERSION = "1.0.0"

    /**
     * 计算稳定性指标。
     *
     * @param track 音高轨迹（领域视图；后处理或原始轨迹均可）
     * @return [PitchStabilityMetricsResult]
     */
    fun compute(track: PitchAnalysisTrack): PitchStabilityMetricsResult {
        val voiced = track.voicedFrames
        if (voiced.isEmpty()) {
            return PitchStabilityMetricsResult(0.0, 0.0, 0.0, 0.0)
        }

        // 稳定片段（两遍法，见类注释）
        val runs = stableRuns(voiced)
        val stableCount =
            runs
                .filter { it.size >= AnalysisConfig.MIN_STABLE_RUN_FRAMES }
                .sumOf { it.size }
        val stableFrameRatio = stableCount.toDouble() / voiced.size

        // 音分中位绝对偏差
        val cents = voiced.map { it.midiNote * 100.0 }
        val medianCents = median(cents)
        val pitchDeviationCents = median(cents.map { abs(it - medianCents) })

        // 长音片段内 F0 抖动（平均绝对偏差）
        val longRuns = runs.filter { runDurationMs(it, voiced) >= AnalysisConfig.LONG_NOTE_DURATION_MS }
        val longNoteDeviationCents =
            if (longRuns.isEmpty()) {
                0.0
            } else {
                val devs =
                    longRuns.flatMap { run ->
                        val runMedian = median(run.map { voiced[it].midiNote * 100.0 })
                        run.map { abs(voiced[it].midiNote * 100.0 - runMedian) }
                    }
                devs.sum() / devs.size
            }

        val voicedFrameRatio =
            if (track.frames.isEmpty()) {
                0.0
            } else {
                voiced.size.toDouble() / track.frames.size
            }

        return PitchStabilityMetricsResult(
            stableFrameRatio,
            pitchDeviationCents,
            longNoteDeviationCents,
            voicedFrameRatio,
        )
    }

    /**
     * 稳定片段切分（两遍法）：
     * 1. 相邻帧差 > ±50 音分即切断；
     * 2. 剔除相对片段中位数偏差 > ±50 音分的帧。
     */
    private fun stableRuns(voiced: List<PitchAnalysisFrame>): List<List<Int>> {
        val raw = mutableListOf<MutableList<Int>>()
        for (i in voiced.indices) {
            val joinPrev =
                raw.isNotEmpty() &&
                    abs(voiced[i].midiNote - voiced[i - 1].midiNote) * 100.0 <=
                    AnalysisConfig.STABLE_CENTS_TOLERANCE
            if (joinPrev) raw.last().add(i) else raw.add(mutableListOf(i))
        }
        return raw.map { run ->
            val runCents = run.map { voiced[it].midiNote * 100.0 }
            val runMedian = median(runCents)
            run.filter { abs(voiced[it].midiNote * 100.0 - runMedian) <= AnalysisConfig.STABLE_CENTS_TOLERANCE }
        }
    }

    /**
     * 片段时长（毫秒）：时间戳跨度 + 一个帧周期（补上首帧时长）；
     * 时间戳退化（全 0 等）时按帧数折算 [推测]。
     */
    private fun runDurationMs(
        run: List<Int>,
        voiced: List<PitchAnalysisFrame>,
    ): Long {
        val first = voiced[run.first()].timestampMs
        val last = voiced[run.last()].timestampMs
        return if (last > first) {
            last - first + AnalysisConfig.FRAME_PERIOD_MS
        } else {
            run.size.toLong() * AnalysisConfig.FRAME_PERIOD_MS
        }
    }

    /** 中位数（偶数个取中间两值平均）。 */
    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return Double.NaN
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    }
}

/**
 * M5.5 音高稳定性指标结果（字段对齐 data-model §2.7 VoiceFeatureVector）。
 */
data class PitchStabilityMetricsResult(
    /** 稳定帧比例 [0,1]（稳定片段帧数 / 总有效帧数）。 */
    val stableFrameRatio: Double,
    /** 音高波动（音分，有效帧音分的中位绝对偏差）。 */
    val pitchDeviationCents: Double,
    /** 长音波动（音分，长音片段内 F0 平均绝对偏差）。 */
    val longNoteDeviationCents: Double,
    /** 有效帧比例 [0,1]（有效帧 / 总帧）。 */
    val voicedFrameRatio: Double,
    /** 算法版本（语义化）。 */
    val methodVersion: String = PitchStabilityMetrics.METHOD_VERSION,
)
