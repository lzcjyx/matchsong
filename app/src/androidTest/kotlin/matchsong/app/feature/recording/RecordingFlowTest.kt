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
        // BUG-020：授权后不再自动前进——等待 GRANTED 分支（提示"麦克风已授权"），
        // 再点一次按钮显式进入录音页。倒计时（BUG-013 真实 3→2→1）与录音中均在真机
        // 渲染；本模拟器无音频输入但录音机可初始化（采集为静音）。权限回调在测试环境
        // 偶有延迟，等待窗口放宽。
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("麦克风已授权，点击开始录音").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("开始录音").performClick()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithText("倒计时 3…").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("录音中…").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
