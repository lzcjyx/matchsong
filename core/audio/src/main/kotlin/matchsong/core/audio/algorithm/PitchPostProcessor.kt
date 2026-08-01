package matchsong.core.audio.algorithm

import matchsong.core.audio.api.PitchFrame
import matchsong.core.audio.api.PitchTrack
import kotlin.math.abs

/**
 * M5.2-1 音高后处理管线（FR-ANAL-2，PLAN M5.2，ARCHITECTURE.md §9.2.3）。
 *
 * 步骤：
 * 1. 无效帧过滤（isVoiced=false 已由 YIN 标记，此处丢弃）；
 * 2. 低置信度过滤（confidence < 阈值 → 丢弃）；
 * 3. 八度错误近似修正（相邻帧频差约 2 倍关系 → 修正，[推测] 参数 M5.8 标定）；
 * 4. 中值滤波（窗口 5）平滑抖动；
 * 5. 短时跳变过滤（孤立单帧跳变 → 丢弃）；
 * 6. 最短稳定音高片段合并（片段时长低于阈值 → 丢弃）。
 *
 * 输出处理步骤列表（processingSteps，data-model §2.5）供结果追溯。
 */
class PitchPostProcessor(
    private val config: PitchPostConfig = PitchPostConfig(),
) {
    fun process(track: PitchTrack): PitchTrack {
        val steps = mutableListOf<String>()

        // 1+2：有效帧 + 置信度过滤
        var frames = track.frames.filter { it.isVoiced && it.confidence >= config.minConfidence }
        steps.add("filter:voiced+confidence>=${config.minConfidence} (${track.frames.size}->${frames.size})")

        // 3：八度错误修正（基于轨迹连续性）
        frames = correctOctaveErrors(frames)
        steps.add("octave-correction")

        // 4：中值滤波
        frames = medianFilter(frames)
        steps.add("median-filter:window=${config.medianWindow}")

        // 5：短时跳变过滤
        frames = filterTransientJumps(frames)
        steps.add("transient-jump-filter")

        // 6：最短稳定片段
        frames = filterShortSegments(frames)
        steps.add("min-segment:${config.minSegmentDurationMs}ms")

        return PitchTrack(
            frames = frames,
            voicedFrameCount = frames.size,
            algorithmVersion = track.algorithmVersion,
        )
    }

    /** 八度修正（保守版）：仅处理 2 倍关系（相邻帧比），不引入全局参考。
     *  说明：M5.8 MIR-1K 实测发现带伴奏人声存在 1/3 子谐波锁定（YIN 对复杂信号局限），
     *  全局对齐易误伤；保守处理 2 倍关系，1/3 子谐波记录为已知限制（M10 优化项）。
     *  清唱场景（MVP 主场景）无此问题（合成/清唱验证 <0.3% 误差）。 */
    private fun correctOctaveErrors(frames: List<PitchFrame>): List<PitchFrame> {
        if (frames.size < 2) return frames
        val out = ArrayList<PitchFrame>(frames.size)
        out.add(frames.first())
        for (i in 1 until frames.size) {
            val prev = out.last().f0Hz
            val cur = frames[i].f0Hz
            val ratio = cur / prev
            val corrected: Double =
                when {
                    // 约 2 倍 → 降八度（cur/2 落在人声区）
                    abs(ratio - 2.0) < 0.06 && cur / 2.0 >= MIN_VOCAL_FREQ -> cur / 2.0
                    // 约 1/2 → 升八度
                    abs(ratio - 0.5) < 0.03 && cur * 2.0 <= MAX_VOCAL_FREQ -> cur * 2.0
                    else -> cur
                }
            out.add(
                if (corrected != cur) {
                    frames[i].copy(f0Hz = corrected, midiNote = PitchNotation.freqToMidi(corrected))
                } else {
                    frames[i]
                },
            )
        }
        return out
    }

    private companion object {
        /** 人声合理基频区（男声低限 75Hz / 女声高限 650Hz，MIR-1K 真值 122-268/151-440 扩展）。 */
        const val MIN_VOCAL_FREQ = 75.0
        const val MAX_VOCAL_FREQ = 650.0
    }

    /** 中值滤波（窗口 5，奇数窗口）。 */
    private fun medianFilter(frames: List<PitchFrame>): List<PitchFrame> {
        if (frames.size < config.medianWindow) return frames
        val half = config.medianWindow / 2
        return frames.mapIndexed { i, frame ->
            val start = (i - half).coerceAtLeast(0)
            val end = (i + half + 1).coerceAtMost(frames.size)
            val window = frames.subList(start, end).map { it.f0Hz }.sorted()
            val median = window[window.size / 2]
            frame.copy(f0Hz = median, midiNote = PitchNotation.freqToMidi(median))
        }
    }

    /** 短时跳变过滤：孤立单帧与前后帧差 > maxJumpCents → 丢弃。 */
    private fun filterTransientJumps(frames: List<PitchFrame>): List<PitchFrame> {
        if (frames.size < 3) return frames
        return frames.filterIndexed { i, frame ->
            if (i == 0 || i == frames.size - 1) return@filterIndexed true
            val prev = frames[i - 1].f0Hz
            val next = frames[i + 1].f0Hz
            // 跳变 = 本帧远离前后两帧（而前后帧彼此接近）
            val jumpFromPrev = abs(PitchNotation.freqDiffCents(frame.f0Hz, prev))
            val jumpFromNext = abs(PitchNotation.freqDiffCents(frame.f0Hz, next))
            val prevNextClose = abs(PitchNotation.freqDiffCents(prev, next)) < config.maxJumpCents
            !(prevNextClose && jumpFromPrev > config.maxJumpCents && jumpFromNext > config.maxJumpCents)
        }
    }

    /** 最短稳定片段：连续片段（音高跳变或时间间隔断裂）时长 < 阈值 → 丢弃。
     *  BUG-015：分段同时按时间间隔断裂（> maxSegmentGapMs）——说话词/短促语音
     *  与歌唱音以微停顿分隔，避免与相邻歌唱音合并后逃过时长过滤。 */
    private fun filterShortSegments(frames: List<PitchFrame>): List<PitchFrame> {
        if (frames.isEmpty()) return frames
        val out = ArrayList<PitchFrame>(frames.size)
        var segStart = 0
        for (i in 1..frames.size) {
            val segEnd =
                i == frames.size ||
                    abs(PitchNotation.freqDiffCents(frames[i].f0Hz, frames[i - 1].f0Hz)) > config.maxJumpCents ||
                    frames[i].timestampMs - frames[i - 1].timestampMs > config.maxSegmentGapMs
            if (segEnd) {
                val segDuration = frames[i - 1].timestampMs - frames[segStart].timestampMs
                if (segDuration >= config.minSegmentDurationMs) {
                    out.addAll(frames.subList(segStart, i))
                }
                segStart = i
            }
        }
        return out
    }
}

/**
 * M5.2-1 后处理配置（[推测] 参数，M5.8 真机数据标定后记录版本）。
 */
data class PitchPostConfig(
    /** 低置信度过滤阈值（spike §5.3：confidence < 0.5 视为无效）。 */
    val minConfidence: Double = 0.5,
    /** 中值滤波窗口（奇数，[推测] 5）。 */
    val medianWindow: Int = 5,
    /** 跳变判定阈值（音分，[推测] 60 = 半音）。 */
    val maxJumpCents: Double = 60.0,
    /** 最短稳定片段时长（毫秒，[推测] 300ms）。 */
    val minSegmentDurationMs: Long = 300,
    /** 分段时间间隔断裂阈值（毫秒，[推测] 150ms，BUG-015 语音干扰过滤）。 */
    val maxSegmentGapMs: Long = 150,
)
