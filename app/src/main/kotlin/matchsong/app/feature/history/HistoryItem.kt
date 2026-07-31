package matchsong.app.feature.history

import matchsong.core.audio.algorithm.PitchNotation
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.port.AnalysisSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * M8.4-2 历史列表条目展示格式化（时间 / 稳定音域摘要 / 置信度）。
 */

// 记录时间展示格式（本地时区，分钟精度）（本地时区，分钟精度）。 */
private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

// 记录时间展示文案（epoch 毫秒 → 本地时间）
fun AnalysisSummary.timeText(): String =
    TIME_FORMATTER.format(Instant.ofEpochMilli(analyzedAtMs).atZone(ZoneId.systemDefault()))

/** 稳定音域摘要（音名通俗展示，PitchNotation 与结果页同源）；无稳定音域 → 降级文案。 */
fun AnalysisSummary.rangeSummaryText(): String {
    val low = stableLowestMidi
    val high = stableHighestMidi
    return if (low != null && high != null) {
        "${PitchNotation.midiToNoteName(low)} – ${PitchNotation.midiToNoteName(high)}"
    } else {
        "数据不足，未生成稳定音域"
    }
}

/** 置信度徽标文案（SPEC §13 分档）；null = 占位未记录。 */
fun AnalysisSummary.confidenceLabelText(): String =
    when (confidenceLevel) {
        ConfidenceLevel.HIGH -> "高"
        ConfidenceLevel.MEDIUM -> "中"
        ConfidenceLevel.LOW -> "低"
        null -> "未记录"
    }
