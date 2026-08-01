package matchsong.data.local.settings

import kotlinx.coroutines.runBlocking
import matchsong.domain.port.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * M9.3 DataStore 设置仓库测试（FR-PRIV-5：删除流程可测；ACC-15 重置语义）。
 *
 * Robolectric JVM 测试：验证 save → clear 后设置恢复默认、Onboarding 标记清除。
 * 注意：DataStore 单例按进程缓存，本类只用一个测试方法（自包含完整生命周期），
 * 避免跨用例文件残留导致状态污染。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreSettingsRepositoryTest {
    @Test
    fun `clear 清空设置与 Onboarding 标记并恢复默认`() =
        runBlocking {
            val repo = DataStoreSettingsRepository(RuntimeEnvironment.getApplication())

            // 写入非默认值
            repo.saveSettings(
                UserSettings(
                    language = "en",
                    preferredGenres = listOf("pop", "rock"),
                    excludedGenres = listOf("jazz"),
                ),
            )
            repo.setOnboardingCompleted(true)
            assertTrue(repo.isOnboardingCompleted())

            // 删除全部数据 → 恢复默认 + 标记清除（ACC-15 重新 Onboarding）
            repo.clear()

            val cleared = repo.getSettings()
            assertEquals("zh", cleared.language)
            assertTrue(cleared.preferredGenres.isEmpty())
            assertTrue(cleared.excludedGenres.isEmpty())
            assertFalse(repo.isOnboardingCompleted())
        }
}
