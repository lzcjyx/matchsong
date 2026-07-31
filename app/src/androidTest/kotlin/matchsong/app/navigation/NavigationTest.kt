package matchsong.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import matchsong.app.MainActivity
import matchsong.core.testing.fake.FakeConsentRepository
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * M2.1-3 / M2.5-2 导航测试：正常导航、返回键、页面可达性（PLAN M2.1 测试项）。
 *
 * 注意：Android DEX 不允许方法名含空格，测试方法统一用驼峰命名。
 * 前置：debug DI Fake（未同意）→ 先完成 Onboarding 进入首页。
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun resetConsent() {
        // Hilt Singleton 的 FakeConsentRepository 跨测试共享状态；
        // Activity 在 Rule apply 时启动（早于 @Before），故在 @After 重置保证下一测试干净。
        FakeConsentRepository.resetAll()
    }

    private fun completeOnboarding() {
        composeRule.onNodeWithText("同意并继续").performClick()
        composeRule.onNodeWithText("开始测试").assertIsDisplayed()
    }

    @Test
    fun homeToPrepareToRecordingFlow() {
        completeOnboarding()
        composeRule.onNodeWithText("开始测试").performClick()
        composeRule.onNodeWithText("录音准备").assertIsDisplayed()
        composeRule.onNodeWithText("开始录音").performClick()
        composeRule.onNodeWithText("模拟录音中…").assertIsDisplayed()
    }

    @Test
    fun fullFakeFlowReachesRecommendationList() {
        completeOnboarding()
        composeRule.onNodeWithText("开始测试").performClick()
        composeRule.onNodeWithText("开始录音").performClick()
        composeRule.onNodeWithText("停止录音").performClick()
        composeRule.onNodeWithText("开始分析").performClick()
        composeRule.onNodeWithText("查看结果（模拟）").performClick()
        composeRule.onNodeWithText("查看推荐歌曲").performClick()
        composeRule.onNodeWithText("推荐歌曲").assertIsDisplayed()
        composeRule.onNodeWithText("晴天").assertIsDisplayed()
    }

    @Test
    fun recommendationDetailShowsSongAndBackReturns() {
        completeOnboarding()
        composeRule.onNodeWithText("开始测试").performClick()
        composeRule.onNodeWithText("开始录音").performClick()
        composeRule.onNodeWithText("停止录音").performClick()
        composeRule.onNodeWithText("开始分析").performClick()
        composeRule.onNodeWithText("查看结果（模拟）").performClick()
        composeRule.onNodeWithText("查看推荐歌曲").performClick()
        composeRule.onNodeWithText("晴天").performClick()
        composeRule.onNodeWithText("周杰伦").assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithText("推荐歌曲").assertIsDisplayed()
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
