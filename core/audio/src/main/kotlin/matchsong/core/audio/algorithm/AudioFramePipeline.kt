package matchsong.core.audio.algorithm

/**
 * M4.1-1 分析帧（质量检测与 YIN 共用，ADR-003：帧长 2048 / hop 1024 @44.1k）。
 */
data class Frame(
    /** 帧起始样本索引（原始 PCM 中）。 */
    val startSample: Int,
    /** 帧内归一化样本（-1.0..1.0，窗函数处理后）。 */
    val samples: FloatArray,
    /** 帧时间戳（相对录音起点，毫秒，data-model §2.4）。 */
    val timestampMs: Long,
    /** 帧统计（M4.1-1）。 */
    val stats: FrameStats,
)

/**
 * 帧统计（供质量检测与音量反馈复用）。
 */
data class FrameStats(
    val rms: Double,
    val peak: Double,
    /** 连续满幅样本最大游程（Q-3 削波判定输入）。 */
    val maxConsecutiveFullScale: Int,
    /** 过零率（相邻样本变号比例；噪声判定特征，[推测] M4.6-2）。 */
    val zeroCrossingRate: Double,
)

/**
 * M4.1-1 帧分割：输入归一化样本流 → Frame 序列。
 *
 * - 帧长 2048、hop 1024（50% 重叠，ADR-003）；
 * - 尾部不足一帧的样本丢弃（质量与分析两端统一策略）；
 * - 帧统计在加窗前计算？否——统计在原始样本上计算，窗函数仅用于频谱类分析（YIN）。
 *   约定：Frame.samples 为原始样本副本（未加窗），YIN 内部自行加窗（与 Spike 一致）。
 */
object AudioFramePipeline {
    const val FRAME_SIZE = 2048
    const val HOP_SIZE = 1024
    const val SAMPLE_RATE = 44100

    /**
     * 分帧并计算统计。
     *
     * @param samples 归一化样本（-1.0..1.0）。
     * @param sampleRateHz 采样率（默认 44100）。
     * @return Frame 列表；样本不足一帧时返回空。
     */
    fun process(
        samples: FloatArray,
        sampleRateHz: Int = SAMPLE_RATE,
    ): List<Frame> {
        if (samples.size < FRAME_SIZE) return emptyList()
        val frames = ArrayList<Frame>((samples.size - FRAME_SIZE) / HOP_SIZE + 1)
        var start = 0
        while (start + FRAME_SIZE <= samples.size) {
            val frameSamples = samples.copyOfRange(start, start + FRAME_SIZE)
            frames.add(
                Frame(
                    startSample = start,
                    samples = frameSamples,
                    timestampMs = start * 1000L / sampleRateHz,
                    stats = computeStats(frameSamples),
                ),
            )
            start += HOP_SIZE
        }
        return frames
    }

    /** 帧统计：RMS / 峰值 / 连续满幅最大游程 / 过零率。 */
    fun computeStats(frame: FloatArray): FrameStats {
        var sumSq = 0.0
        var peak = 0.0
        var run = 0
        var maxRun = 0
        var crossings = 0
        for (i in frame.indices) {
            val v = frame[i]
            val abs = kotlin.math.abs(v)
            sumSq += v.toDouble() * v.toDouble()
            if (abs > peak) peak = abs.toDouble()
            if (abs >= FULL_SCALE) {
                run++
                if (run > maxRun) maxRun = run
            } else {
                run = 0
            }
            if (i > 0) {
                val prev = frame[i - 1]
                val falling = prev < 0f && v >= 0f
                val rising = prev > 0f && v <= 0f
                if (falling || rising) crossings++
            }
        }
        return FrameStats(
            rms = kotlin.math.sqrt(sumSq / frame.size),
            peak = peak,
            maxConsecutiveFullScale = maxRun,
            zeroCrossingRate = crossings.toDouble() / frame.size,
        )
    }

    /** 满幅样本判定阈值（归一化；PCM16 满幅 ≈ 0.99997，取 0.999 保守值，data-model Q-3）。 */
    const val FULL_SCALE = 0.999f
}
