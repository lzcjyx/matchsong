package matchsong.core.audio.algorithm

import matchsong.core.audio.api.AudioFrameSource

/**
 * M4.4-1 质量分析器（FR-QUAL-1/3，PLAN M4.4）。
 *
 * 输入 [AudioFrameSource]（M4.1-2）→ 输出 [AudioQualityReport]。
 * 门禁判定顺序（多原因并存时按此优先级，集中定义）：
 * TOO_SHORT → SILENT → TOO_QUIET → NOISY → CLIPPING → INSUFFICIENT_VOICE
 *
 * 任一命中 → isUsable=false、recommendedAction=RETRY（ACC-7/8）。
 * 纯函数，JVM 可测；阈值经 [QualityConfig] 注入。
 */
class QualityAnalyzer(
    private val config: QualityConfig = QualityConfig.DEFAULTS,
) {
    fun analyze(source: AudioFrameSource): AudioQualityReport {
        val frames = source.readFrames()
        if (frames.isEmpty()) {
            return unusable(
                durationMs = 0,
                warnings = listOf(QualityWarning.TOO_SHORT),
                frames = emptyList(),
            )
        }

        val durationMs = frames.last().timestampMs + frameDurationMs(frames)
        val total = frames.size
        var silent = 0
        var quiet = 0
        var clipped = 0
        var active = 0
        var sumRms = 0.0
        var peak = 0.0
        val allRmsValues = ArrayList<Double>(total)

        for (f in frames) {
            val rms = f.stats.rms
            val isSilent = rms <= config.silenceRmsThreshold
            val isQuiet = rms < config.quietRmsThreshold
            val isClipped = f.stats.maxConsecutiveFullScale >= config.clippingConsecutiveFullScaleSamples

            if (isSilent) silent++
            if (isQuiet) quiet++
            if (isClipped) clipped++
            if (!isSilent && !isClipped) active++
            sumRms += rms
            if (f.stats.peak > peak) peak = f.stats.peak
            allRmsValues.add(rms)
        }

        val silenceRatio = silent.toDouble() / total
        val quietRatio = quiet.toDouble() / total
        val clippingRatio = clipped.toDouble() / total
        val activeRatio = active.toDouble() / total
        val averageRms = sumRms / total
        val activeDurationMs = active * frameDurationMs(frames)
        val noiseEstimate = estimateNoise(allRmsValues)
        // 平均过零率：白噪声 ≈ 0.5；正弦/浊音远低于 0.1（[推测] 阈值 0.3，M4.6-2 标定）
        val averageZcr = frames.map { it.stats.zeroCrossingRate }.average()

        // 门禁判定（顺序即优先级）
        val warnings =
            buildList {
                if (durationMs < config.minDurationMs) add(QualityWarning.TOO_SHORT)
                if (silenceRatio >= 0.95) add(QualityWarning.SILENT) // [推测] 纯静音判定
                if (quietRatio >= 0.8 && silenceRatio < 0.95) add(QualityWarning.TOO_QUIET) // [推测]
                // 噪声判定 [推测]：平均过零率过高（> 0.3）→ 无周期性 → 环境嘈杂/非人声
                if (averageRms > 0.02 && averageZcr > 0.3) {
                    add(QualityWarning.NOISY)
                }
                if (clippingRatio >= config.clippingRatioLimit) add(QualityWarning.CLIPPING)
                if (activeRatio < config.minActiveFrameRatio || activeDurationMs < config.minActiveVoiceDurationMs) {
                    add(QualityWarning.INSUFFICIENT_VOICE)
                }
            }

        val isUsable = warnings.isEmpty()
        val confidence =
            if (isUsable) {
                // 可用但置信度：有效帧比例加权（≥0.8 高置信；否则中置信）[推测]
                (activeRatio / 0.8).coerceIn(0.5, 1.0)
            } else {
                0.0
            }

        return AudioQualityReport(
            isUsable = isUsable,
            confidence = confidence,
            durationMs = durationMs,
            silenceRatio = silenceRatio,
            quietRatio = quietRatio,
            clippingRatio = clippingRatio,
            averageRms = averageRms,
            peak = peak,
            activeRatio = activeRatio,
            noiseEstimate = noiseEstimate,
            analyzableFrameCount = total,
            vocalActivityRanges = estimateActiveRanges(frames, config),
            warnings = warnings,
            recommendedAction = if (isUsable) QualityAction.ANALYZE else QualityAction.RETRY,
            qualityVersion = "1.0",
        )
    }

    private fun frameDurationMs(frames: List<Frame>): Long {
        // 帧长 2048 @44.1k ≈ 46.4ms；用帧间时间戳差推算（更精确）
        if (frames.size < 2) return (AudioFramePipeline.FRAME_SIZE * 1000L / AudioFramePipeline.SAMPLE_RATE)
        return frames[1].timestampMs - frames[0].timestampMs
    }

    /** 噪声估计：全帧 RMS 中位数 [推测]（高底噪信号中位数接近均值）。 */
    private fun estimateNoise(rmsValues: List<Double>): Double =
        if (rmsValues.isEmpty()) 0.0 else rmsValues.sorted()[rmsValues.size / 2]

    /** 有效演唱活动区间（连续有效帧合并为区间）[推测]。 */
    private fun estimateActiveRanges(
        frames: List<Frame>,
        config: QualityConfig,
    ): List<Pair<Long, Long>> {
        val ranges = ArrayList<Pair<Long, Long>>()
        var start: Long? = null
        var lastEnd = 0L
        for (f in frames) {
            val active =
                f.stats.rms > config.silenceRmsThreshold &&
                    f.stats.maxConsecutiveFullScale < config.clippingConsecutiveFullScaleSamples
            if (active && start == null) {
                start = f.timestampMs
            }
            if (!active && start != null) {
                ranges.add(start to lastEnd)
                start = null
            }
            if (active) {
                lastEnd = f.timestampMs + (AudioFramePipeline.FRAME_SIZE * 1000L / AudioFramePipeline.SAMPLE_RATE)
            }
        }
        start?.let { ranges.add(it to lastEnd) }
        return ranges
    }

    private fun unusable(
        durationMs: Long,
        warnings: List<QualityWarning>,
        frames: List<Frame>,
    ): AudioQualityReport =
        AudioQualityReport(
            isUsable = false,
            confidence = 0.0,
            durationMs = durationMs,
            silenceRatio = if (frames.isEmpty()) 1.0 else 0.0,
            quietRatio = if (frames.isEmpty()) 1.0 else 0.0,
            clippingRatio = 0.0,
            averageRms = 0.0,
            peak = 0.0,
            activeRatio = 0.0,
            noiseEstimate = 0.0,
            analyzableFrameCount = frames.size,
            vocalActivityRanges = emptyList(),
            warnings = warnings,
            recommendedAction = QualityAction.RETRY,
            qualityVersion = "1.0",
        )
}
