package matchsong.domain.recording

import kotlinx.coroutines.test.runTest
import matchsong.domain.port.RecordingFileCleaner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M3.5-2 过期残留清理用例测试（FR-REC-8）。
 *
 * 用 Fake Port 验证契约：过期且非活跃会话文件被删、活跃会话与新鲜文件保留、
 * 空集合无事发生；参数按调用原样透传。
 */
class CleanupStaleRecordingsUseCaseTest {
    /** 模拟 Port 契约的 Fake：{文件名 -> 修改时间}，删除过期且非活跃会话文件。 */
    private class FakeRecordingFileCleaner(
        initial: Map<String, Long>,
    ) : RecordingFileCleaner {
        val files = initial.toMutableMap()
        var lastOlderThanMs: Long? = null
        var lastActiveSessionIds: Set<String>? = null

        override suspend fun cleanStale(
            olderThanMs: Long,
            activeSessionIds: Set<String>,
        ): Int {
            lastOlderThanMs = olderThanMs
            lastActiveSessionIds = activeSessionIds
            val cutoff = System.currentTimeMillis() - olderThanMs
            val stale =
                files.filter { (name, lastModified) ->
                    val sessionId = name.substringBeforeLast('.')
                    lastModified < cutoff && sessionId !in activeSessionIds
                }
            stale.keys.forEach { files.remove(it) }
            return stale.size
        }
    }

    private fun now() = System.currentTimeMillis()

    private val oneHourMs = 60 * 60 * 1000L
    private val twoHoursMs = 2 * 60 * 60 * 1000L

    @Test
    fun `过期且非活跃会话文件被删除，活跃会话文件保留`() =
        runTest {
            val now = now()
            val fake =
                FakeRecordingFileCleaner(
                    mapOf(
                        // 过期 + 非活跃 → 删
                        "a.pcm" to now - twoHoursMs,
                        // 过期 + 非活跃 → 删
                        "b.wav" to now - twoHoursMs,
                        // 新鲜 → 保留
                        "c.pcm" to now,
                        // 过期但活跃 → 保留
                        "d.pcm" to now - twoHoursMs,
                    ),
                )
            val useCase = CleanupStaleRecordingsUseCase(fake)

            val deleted = useCase(olderThanMs = oneHourMs, activeSessionIds = setOf("d"))

            assertEquals(2, deleted)
            assertTrue("a.pcm" !in fake.files)
            assertTrue("b.wav" !in fake.files)
            assertTrue("c.pcm" in fake.files)
            assertTrue("d.pcm" in fake.files)
        }

    @Test
    fun `无文件时返回 0 且不产生删除`() =
        runTest {
            val fake = FakeRecordingFileCleaner(emptyMap())
            val useCase = CleanupStaleRecordingsUseCase(fake)

            assertEquals(0, useCase())
            assertTrue(fake.files.isEmpty())
        }

    @Test
    fun `olderThanMs 与 activeSessionIds 原样透传 Port`() =
        runTest {
            val fake = FakeRecordingFileCleaner(emptyMap())
            val useCase = CleanupStaleRecordingsUseCase(fake)

            useCase(olderThanMs = 1234L, activeSessionIds = setOf("x", "y"))

            assertEquals(1234L, fake.lastOlderThanMs)
            assertEquals(setOf("x", "y"), fake.lastActiveSessionIds)
        }

    @Test
    fun `默认过期阈值为 24 小时`() =
        runTest {
            val fake = FakeRecordingFileCleaner(emptyMap())
            val useCase = CleanupStaleRecordingsUseCase(fake)

            useCase()

            assertEquals(CleanupStaleRecordingsUseCase.DEFAULT_STALE_MS, fake.lastOlderThanMs)
            assertTrue(fake.lastActiveSessionIds.orEmpty().isEmpty())
        }
}
