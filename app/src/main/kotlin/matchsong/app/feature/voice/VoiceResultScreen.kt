package matchsong.app.feature.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import matchsong.app.design.MatchSongSpacing
import matchsong.app.design.components.PrimaryButton
import matchsong.app.design.components.state.EmptyState
import matchsong.core.audio.algorithm.PitchNotation
import matchsong.domain.analysis.AnalysisWarning
import matchsong.domain.analysis.ComfortRangeEstimate
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.analysis.PitchStabilityMetricsResult
import matchsong.domain.analysis.VocalRangeEstimate
import matchsong.domain.analysis.VoiceAnalysisResult

/**
 * M5.7-1 声音结果页（FR-ANAL-7，ACC-10）。
 *
 * - 稳定音域/舒适音区（音名通俗展示）+ 置信度徽标 + 算法版本；
 * - "本次录音估计"声明置顶（P7）；
 * - 数据不足（LOW/无音域）→ 提示重录（ACC-9）；
 * - MEDIUM → "基于有限样本"标注（SPEC §13）。
 */
@Suppress("LongMethod") // Compose 页面声明式布局（83 行），拆散破坏可读性
@Composable
fun VoiceResultScreen(
    onSeeRecommendations: () -> Unit,
    onRetry: () -> Unit,
    result: VoiceAnalysisResult = DemoVoiceResult,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "本次录音估计",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = MatchSongSpacing.M),
        )
        Text(text = "你的声音分析", style = MaterialTheme.typography.headlineMedium)

        val range = result.vocalRange
        if (range == null || !range.sampleSufficiency || result.confidenceLevel == ConfidenceLevel.LOW) {
            // 数据不足（ACC-9）：不输出音域，提示重录
            EmptyState(
                text = "有效演唱片段不足，无法可靠估计音域",
                actionText = "重新录制",
                onAction = onRetry,
            )
            return
        }

        // 稳定音域
        range.stableLowestMidi?.let { low ->
            range.stableHighestMidi?.let { high ->
                Text(
                    text = "本次稳定音域 ${PitchNotation.midiToNoteName(low)} – ${PitchNotation.midiToNoteName(high)}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = MatchSongSpacing.M),
                )
            }
        }

        // 舒适音区
        result.comfortRange?.let { comfort ->
            comfortText(comfort)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = MatchSongSpacing.S),
                )
            }
        }

        // 置信度徽标
        Row(modifier = Modifier.padding(top = MatchSongSpacing.S)) {
            Text(
                text = "置信度：${confidenceLabel(result.confidenceLevel)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (result.confidenceLevel == ConfidenceLevel.MEDIUM) {
                Text(
                    text = "（基于有限样本）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = MatchSongSpacing.S),
                )
            }
        }

        // 稳定性（通俗展示，非裸指标）
        result.stability?.let {
            stabilityText(it)?.let { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = MatchSongSpacing.S),
                )
            }
        }

        Text(
            text = "算法版本 ${result.algorithmVersion}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = MatchSongSpacing.M),
        )

        PrimaryButton(
            text = "查看推荐歌曲",
            onClick = onSeeRecommendations,
            modifier = Modifier.padding(top = MatchSongSpacing.L),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = MatchSongSpacing.S)) {
            Text("重新录制")
        }
    }
}

private fun comfortText(comfort: ComfortRangeEstimate): String? {
    val low = comfort.comfortLowestMidi ?: return null
    val high = comfort.comfortHighestMidi ?: return null
    return "舒适音区 ${PitchNotation.midiToNoteName(low)} – ${PitchNotation.midiToNoteName(high)}"
}

private fun stabilityText(stability: PitchStabilityMetricsResult): String? {
    val stableRatio = stability.stableFrameRatio
    return if (stableRatio >= 0.6) {
        "音高稳定性：较好（大部分音符稳定）"
    } else if (stableRatio >= 0.3) {
        "音高稳定性：一般（部分音符有波动）"
    } else {
        "音高稳定性：偏弱（波动较多）"
    }
}

private fun confidenceLabel(level: ConfidenceLevel): String =
    when (level) {
        ConfidenceLevel.HIGH -> "高"
        ConfidenceLevel.MEDIUM -> "中"
        ConfidenceLevel.LOW -> "低"
    }

/**
 * M5.7 演示结果（M8.2 接入真实分析管线后移除）。
 */
val DemoVoiceResult =
    VoiceAnalysisResult(
        qualityUsable = true,
        qualityWarnings = emptyList(),
        vocalRange =
            VocalRangeEstimate(
                // C3
                stableLowestMidi = 48.0,
                // A4
                stableHighestMidi = 69.0,
                rangeSpanSemitones = 21.0,
                coverage = 0.85,
                confidence = 0.8,
                sampleSufficiency = true,
                warning = AnalysisWarning.NONE,
            ),
        comfortRange =
            ComfortRangeEstimate(
                // E3
                comfortLowestMidi = 52.0,
                // E4
                comfortHighestMidi = 64.0,
                primaryRangeLowMidi = 52.0,
                primaryRangeHighMidi = 64.0,
                confidence = 0.7,
                sampleSufficiency = true,
                estimateDisclaimer = "本次录音估计",
            ),
        stability =
            PitchStabilityMetricsResult(
                stableFrameRatio = 0.75,
                pitchDeviationCents = 35.0,
                longNoteDeviationCents = 20.0,
                voicedFrameRatio = 0.8,
            ),
        voicedFrameCount = 500,
        totalFrameCount = 650,
        confidenceLevel = ConfidenceLevel.HIGH,
        warnings = emptyList(),
        algorithmVersion = "1.0.0",
    )
