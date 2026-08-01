package matchsong.core.testing.fake

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M9.3 FakeRecordingFileCleaner 测试（FR-PRIV-5 删除流程测试用替身契约）。
 */
class FakeRecordingFileCleanerTest {
    @Test
    fun `clearAll 清空全部会话文件并返回数量`() =
        runTest {
            val cleaner = FakeRecordingFileCleaner(setOf("a", "b", "c"))

            assertEquals(3, cleaner.clearAll())
            assertTrue(cleaner.sessionIds.isEmpty())
            assertEquals(0, cleaner.clearAll())
        }

    @Test
    fun `cleanStale 保留活跃会话并透传参数`() =
        runTest {
            val cleaner = FakeRecordingFileCleaner(setOf("a", "b", "c"))

            val deleted = cleaner.cleanStale(olderThanMs = 1000L, activeSessionIds = setOf("a"))

            assertEquals(2, deleted)
            assertEquals(setOf("a"), cleaner.sessionIds)
            assertEquals(1000L, cleaner.lastOlderThanMs)
            assertEquals(setOf("a"), cleaner.lastActiveSessionIds)
        }
}
