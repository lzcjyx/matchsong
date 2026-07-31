package matchsong.app.feature.recommendation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import matchsong.app.design.MatchSongSpacing
import matchsong.domain.port.FeedbackType

/** 反馈类型中文文案（FR-HX-3 六类）。 */
internal val FeedbackType.label: String
    get() =
        when (this) {
            FeedbackType.SUITABLE -> "适合唱"
            FeedbackType.TOO_HIGH -> "太高"
            FeedbackType.TOO_LOW -> "太低"
            FeedbackType.TOO_HARD -> "太难"
            FeedbackType.DISLIKE_STYLE -> "不喜欢该风格"
            FeedbackType.INACCURATE_REASON -> "推荐理由不准确"
        }

/**
 * M8.5-2 反馈底部弹层（FR-HX-3 六类选择）。
 *
 * 用户点选一项后立即回调 [onSubmit] 并切换为「已收到反馈」成功提示（保存成功暗示，
 * 实际持久化由调用方经 SubmitFeedbackUseCase 完成）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    onDismiss: () -> Unit,
    onSubmit: (FeedbackType) -> Unit,
) {
    var submitted by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (submitted) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MatchSongSpacing.L)
                        .navigationBarsPadding(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "已收到你的反馈，谢谢！",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "你的反馈将帮助我们改进推荐。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = MatchSongSpacing.S),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = MatchSongSpacing.M),
                ) {
                    Text("关闭")
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = MatchSongSpacing.L)
                        .navigationBarsPadding(),
            ) {
                Text(
                    text = "这首歌唱起来怎么样？",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = MatchSongSpacing.L, vertical = MatchSongSpacing.M),
                )
                HorizontalDivider()
                FeedbackType.entries.forEachIndexed { index, type ->
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    submitted = true
                                    onSubmit(type)
                                }
                                .padding(horizontal = MatchSongSpacing.L, vertical = MatchSongSpacing.M),
                    )
                    if (index < FeedbackType.entries.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
