package matchsong.app.navigation

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
 * M2.1-3 / M2.5-2 导航测试：正常导航、返回键、页面可达性（PLAN M2.1 测试项）。
 *
 * 说明：真实录音链路（权限回调 → 前台服务 → 录音状态）由异步协程驱动，
 * Compose UI 测试的 TestMainDispatcher 无法推进（已知限制，手动验证已覆盖），
 * 本套件验证到"准备页 + 权限已授予"为界；录音页 UI 测试见 M3.7 人工清单。
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @After
    fun resetConsent() {
        FakeConsentRepository.resetAll()
    }

    private fun completeOnboarding() {
        composeRule.onNodeWithText("同意并继续").performClick()
        composeRule.onNodeWithText("开始测试").assertIsDisplayed()
    }

    /** 进入录音准备页（权限授予后不自动进录音页——异步导航在测试时钟下不推进）。 */
    private fun goToPrepareScreen() {
        completeOnboarding()
        composeRule.onNodeWithText("开始测试").performClick()
        composeRule.onNodeWithText("录音准备").assertIsDisplayed()
        composeRule.onNodeWithText("开始录音").assertIsDisplayed()
    }

    @Test
    fun homeToPrepareFlow() {
        goToPrepareScreen()
    }

    @Test
    fun historyAndSettingsReachableFromHome() {
        completeOnboarding()
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmDialogShowsAndCancels() {
        completeOnboarding()
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("删除全部数据").performClick()
        composeRule.onNodeWithText("删除全部数据？").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()
    }
}
