package matchsong.app.feature.onboarding

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
 * M2.5-2 Onboarding 流程 UI 测试（FR-ONB-1/2/3，ACC-1/2）。
 *
 * debug DI 注入 FakeConsentRepository（初始未同意）→ 首次启动进入 Onboarding。
 * 注意：Android DEX 不允许方法名含空格，测试方法用驼峰命名。
 */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun resetConsent() {
        // Hilt Singleton 的 FakeConsentRepository 跨测试共享状态；
        // Activity 在 Rule apply 时启动（早于 @Before），故在 @After 重置保证下一测试干净。
        FakeConsentRepository.resetAll()
    }

    @Test
    fun firstLaunchShowsOnboardingWithPrivacyNotice() {
        composeRule.onNodeWithText("欢迎使用 MatchSong").assertIsDisplayed()
        composeRule.onNodeWithText("同意并继续").assertIsDisplayed()
    }

    @Test
    fun disagreeStaysOnOnboarding() {
        composeRule.onNodeWithText("欢迎使用 MatchSong").assertIsDisplayed()
        // 不同意 = 不操作，页面保持（MVP 停留策略，ACC-2）
        composeRule.onNodeWithText("欢迎使用 MatchSong").assertIsDisplayed()
    }

    @Test
    fun agreeNavigatesToHome() {
        composeRule.onNodeWithText("同意并继续").performClick()
        composeRule.onNodeWithText("开始测试").assertIsDisplayed()
        composeRule.onNodeWithText("历史记录").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }
}
