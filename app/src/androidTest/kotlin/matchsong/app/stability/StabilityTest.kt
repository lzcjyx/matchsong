package matchsong.app.stability

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
 * M10.4 稳定性测试（可自动化子集，PLAN §16.2 M10.4）。
 *
 * 覆盖：屏幕旋转（Activity 重建后导航/同意状态恢复）、快速重复点击
 * （设置/历史反复进出不崩溃）。录音链路相关稳定性（连续录制/焦点中断/
 * 低内存/存储不足/权限动态撤销）需真机 + 手工清单（docs/testing/manual-test-checklist.md），
 * 模拟器虚拟麦克风与厂商行为无法代理。
 */
@RunWith(AndroidJUnit4::class)
class StabilityTest {
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

    /** 快速重复进出设置/历史（10 轮），验证导航与页面状态稳定。 */
    @Test
    fun rapidNavigationDoesNotCrash() {
        completeOnboarding()
        repeat(10) {
            composeRule.onNodeWithText("设置").performClick()
            composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()
            composeRule.runOnIdle {
                composeRule.activity.onBackPressedDispatcher.onBackPressed()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("开始测试").assertIsDisplayed()

            composeRule.onNodeWithText("历史记录").performClick()
            composeRule.onNodeWithText("历史记录").assertIsDisplayed()
            composeRule.runOnIdle {
                composeRule.activity.onBackPressedDispatcher.onBackPressed()
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("开始测试").assertIsDisplayed()
        }
    }

    /** 屏幕旋转（Activity 重建）：导航栈与会话状态应恢复，不回到 Splash 首页误跳。 */
    @Test
    fun rotationRestoresNavigationState() {
        completeOnboarding()
        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        // 重建后应停留在设置页（导航状态经 SavedState 恢复），而非被 Splash 分流回首页
        composeRule.onNodeWithText("删除全部数据").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }
}
