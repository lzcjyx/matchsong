package matchsong.app.feature.recording

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import matchsong.app.MainActivity
import matchsong.core.testing.fake.FakeConsentRepository
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M3.7-1 录音流程仪器测试（同步可测部分）：
 * 权限授予 → 准备页展示（ACC-3 前置）。
 *
 * 说明：真实录音链路（权限回调 → 前台服务 → 录音状态流）由异步协程驱动，
 * Compose UI 测试的 TestMainDispatcher 无法推进异步导航（已知限制）；
 * 录音核心链路已在模拟器手动验证（手动驱动：录音中 + 前台服务运行 + 停止完成），
 * 见 M3.7-2 人工清单与 M3 验收记录。
 */
@RunWith(AndroidJUnit4::class)
class RecordingFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @After
    fun resetConsent() {
        FakeConsentRepository.resetAll()
    }

    @Test
    fun grantedPermissionShowsPrepareWithRecordButton() {
        composeRule.onNodeWithText("同意并继续").performClick()
        composeRule.onNodeWithText("开始测试").assertIsDisplayed()
        composeRule.onNodeWithText("开始测试").performClick()
        composeRule.onNodeWithText("录音准备").assertIsDisplayed()
        composeRule.onNodeWithText("开始录音").assertIsDisplayed()
    }

    @Test
    fun recordButtonNavigatesToRecordingScreen() {
        composeRule.onNodeWithText("同意并继续").performClick()
        composeRule.onNodeWithText("开始测试").performClick()
        composeRule.onNodeWithText("录音准备").assertIsDisplayed()
        composeRule.onNodeWithText("开始录音").performClick()
        // 权限已授予 → 导航到录音页（倒计时阶段；3s 倒计时由真实 delay 驱动，测试时钟不推进）
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("倒计时 3…").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("录音中…").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
