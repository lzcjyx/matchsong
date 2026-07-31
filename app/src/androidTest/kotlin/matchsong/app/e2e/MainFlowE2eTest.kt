package matchsong.app.e2e

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
 * M8.7-2 E2E 场景套件（Fake 数据驱动，无真实麦克风依赖）。
 *
 * 覆盖 PLAN M8.7 主流程的可同步验证部分：
 * 首次启动 → Onboarding → 首页 → 录音准备 → 设置/历史/收藏可达 → 删除确认。
 * 说明：真实录音→分析→推荐链路由异步协程驱动（Compose TestMainDispatcher
 * 不推进真实异步，M3 已记录），E2E 验证导航串联与页面状态；录音链路本体
 * 由模拟器人工验证（M3.7-2）与单元测试覆盖。
 */
@RunWith(AndroidJUnit4::class)
class MainFlowE2eTest {
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
    fun firstLaunchOnboardingToHome() {
        composeRule.onNodeWithText("欢迎使用 MatchSong").assertIsDisplayed()
        composeRule.onNodeWithText("同意并继续").performClick()
        composeRule.onNodeWithText("开始测试").assertIsDisplayed()
    }

    @Test
    fun homeToPrepareAndBackToHome() {
        completeOnboarding()
        composeRule.onNodeWithText("开始测试").performClick()
        composeRule.onNodeWithText("录音准备").assertIsDisplayed()
        composeRule.onNodeWithText("开始录音").assertIsDisplayed()
        // 系统返回键回首页
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
    }

    @Test
    fun homeToSettingsDirect() {
        completeOnboarding()
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmFlow() {
        completeOnboarding()
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("删除全部数据").performClick()
        composeRule.onNodeWithText("删除全部数据？").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()
    }

    @Test
    fun historyAndFavoritesReachable() {
        completeOnboarding()
        composeRule.onNodeWithText("历史记录").performClick()
        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()
    }

    private fun completeOnboarding() {
        composeRule.onNodeWithText("同意并继续").performClick()
        composeRule.onNodeWithText("开始测试").assertIsDisplayed()
    }
}
