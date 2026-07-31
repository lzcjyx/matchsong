package matchsong.app.design.components.state

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * M2.5-2 状态组件测试：Loading / Empty / Error / QualityWarning 渲染与回调（FR-SHELL-2）。
 * 注意：Android DEX 不允许方法名含空格，测试方法用驼峰命名。
 */
class StateComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateShowsText() {
        composeRule.setContent { LoadingState(text = "加载中…") }
        composeRule.onNodeWithText("加载中…").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsTextAndActionCallback() {
        var clicked = false
        composeRule.setContent {
            EmptyState(text = "暂无数据", actionText = "重试", onAction = { clicked = true })
        }
        composeRule.onNodeWithText("暂无数据").assertIsDisplayed()
        composeRule.onNodeWithText("重试").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun errorStateShowsMessageAndRetryCallback() {
        var retried = false
        composeRule.setContent {
            ErrorState(message = "出错了", onRetry = { retried = true })
        }
        composeRule.onNodeWithText("出错了").assertIsDisplayed()
        composeRule.onNodeWithText("重试").performClick()
        assertEquals(true, retried)
    }

    @Test
    fun qualityWarningMapsAllReasonsToMessages() {
        assertEquals("录音过短，请至少演唱 10 秒", qualityFailureMessage(QualityFailureReason.TOO_SHORT))
        assertEquals("没有检测到声音，请靠近麦克风演唱", qualityFailureMessage(QualityFailureReason.NO_VOICE))
        assertEquals("声音太小，请靠近麦克风或提高音量", qualityFailureMessage(QualityFailureReason.TOO_QUIET))
        assertEquals("环境过于嘈杂，请到安静环境重试", qualityFailureMessage(QualityFailureReason.TOO_NOISY))
        assertEquals("麦克风削波，请降低音量", qualityFailureMessage(QualityFailureReason.CLIPPING))
        assertEquals("有效演唱片段不足，请重新录制", qualityFailureMessage(QualityFailureReason.INSUFFICIENT_ACTIVE))
    }

    @Test
    fun qualityWarningStateShowsReasonAndRetry() {
        var retried = false
        composeRule.setContent {
            QualityWarningState(reason = QualityFailureReason.CLIPPING, onRetry = { retried = true })
        }
        composeRule.onNodeWithText("麦克风削波，请降低音量").assertIsDisplayed()
        composeRule.onNodeWithText("重新录制").performClick()
        assertEquals(true, retried)
    }
}
