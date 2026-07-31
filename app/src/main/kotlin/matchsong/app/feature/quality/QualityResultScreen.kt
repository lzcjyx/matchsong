package matchsong.app.feature.quality

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.PrimaryButton
import matchsong.app.design.components.state.QualityFailureReason
import matchsong.app.design.components.state.QualityWarningState
import matchsong.core.audio.algorithm.AudioQualityReport
import matchsong.core.audio.algorithm.QualityAction
import matchsong.core.audio.algorithm.QualityWarning

/**
 * M4.5-1 音频质量结果页（FR-QUAL-3，ACC-7/8）。
 *
 * - isUsable=true → 报告摘要 + "开始分析"入口（ACC-6）；
 * - isUsable=false → 首个命中警告的原因提示 + 建议重录（QualityWarningState）。
 *
 * M4.5 阶段报告由调用方注入（M8.1 接真实录音文件管线）。
 */
@Composable
fun QualityResultScreen(
    report: AudioQualityReport,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
) {
    if (report.isUsable) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(MatchSongSpacing.L),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "音频质量检测", style = MaterialTheme.typography.headlineMedium)
            Text(
                text =
                    "录音时长 ${report.durationMs / 1000}s\n" +
                        "有效演唱比例 ${(report.activeRatio * 100).toInt()}%\n" +
                        "置信度 ${(report.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = MatchSongSpacing.M),
            )
            if (report.recommendedAction == QualityAction.ANALYZE) {
                PrimaryButton(text = "开始分析", onClick = onAnalyze)
            }
        }
    } else {
        // 首个警告映射为 UI 原因（M2.2-3 文案集中于此）
        val reason = report.warnings.firstOrNull()?.toFailureReason() ?: QualityFailureReason.INSUFFICIENT_ACTIVE
        QualityWarningState(reason = reason, onRetry = onRetry)
    }
}

/** M4.5-1 警告 → UI 原因映射（与 M2.2-3 qualityFailureMessage 配套）。 */
fun QualityWarning.toFailureReason(): QualityFailureReason =
    when (this) {
        QualityWarning.TOO_SHORT -> QualityFailureReason.TOO_SHORT
        QualityWarning.SILENT -> QualityFailureReason.NO_VOICE
        QualityWarning.TOO_QUIET -> QualityFailureReason.TOO_QUIET
        QualityWarning.NOISY -> QualityFailureReason.TOO_NOISY
        QualityWarning.CLIPPING -> QualityFailureReason.CLIPPING
        QualityWarning.INSUFFICIENT_VOICE -> QualityFailureReason.INSUFFICIENT_ACTIVE
    }

/**
 * M4.5 占位报告（演示可用状态；M8.1 接入真实录音文件后移除）。
 */
val DemoQualityReport =
    AudioQualityReport(
        isUsable = true,
        confidence = 0.8,
        durationMs = 15_000,
        silenceRatio = 0.1,
        quietRatio = 0.15,
        clippingRatio = 0.0,
        averageRms = 0.3,
        peak = 0.7,
        activeRatio = 0.85,
        noiseEstimate = 0.02,
        analyzableFrameCount = 600,
        vocalActivityRanges = listOf(0L to 15_000L),
        warnings = emptyList(),
        recommendedAction = QualityAction.ANALYZE,
        qualityVersion = "1.0",
    )
