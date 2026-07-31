package matchsong.app.design.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import matchsong.app.design.MatchSongSpacing

/**
 * M2.2-3 通用状态组件：QualityWarning（质量失败原因 + 建议重录）。
 *
 * 与 M4 的 QualityError 解耦：本组件只消费 [QualityFailureReason] 枚举；
 * M4.5 将 QualityError → 枚举 → 本组件文案。
 */
enum class QualityFailureReason {
    TOO_SHORT,
    NO_VOICE,
    TOO_QUIET,
    TOO_NOISY,
    CLIPPING,
    INSUFFICIENT_ACTIVE,
}

/** M4.5 文案映射（集中于此，业务页不重复）。 */
fun qualityFailureMessage(reason: QualityFailureReason): String =
    when (reason) {
        QualityFailureReason.TOO_SHORT -> "录音过短，请至少演唱 10 秒"
        QualityFailureReason.NO_VOICE -> "没有检测到声音，请靠近麦克风演唱"
        QualityFailureReason.TOO_QUIET -> "声音太小，请靠近麦克风或提高音量"
        QualityFailureReason.TOO_NOISY -> "环境过于嘈杂，请到安静环境重试"
        QualityFailureReason.CLIPPING -> "麦克风削波，请降低音量"
        QualityFailureReason.INSUFFICIENT_ACTIVE -> "有效演唱片段不足，请重新录制"
    }

/**
 * 质量失败提示 + 建议重录。
 */
@Composable
fun QualityWarningState(
    reason: QualityFailureReason,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MatchSongSpacing.L),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = qualityFailureMessage(reason),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = "该录音不能生成可靠的分析结果",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = MatchSongSpacing.S),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = MatchSongSpacing.M)) {
            Text("重新录制")
        }
    }
}
