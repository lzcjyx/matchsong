package matchsong.core.testing

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * TestDispatcherProvider 测试（M1.4-4 验收：确定性调度）。
 */
class TestDispatcherProviderTest {
    @Test
    fun `注入 runTest 调度器后任务确定性执行`() =
        runTest {
            val provider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
            var executed = false
            launch(provider.io) { executed = true }
            assertFalse(executed, "虚拟调度器未推进前任务不应执行")
            advanceUntilIdle()
            assertTrue(executed)
        }

    @Test
    fun `main io default 共享同一虚拟调度器`() {
        val provider = TestDispatcherProvider()
        assertSame(provider.main, provider.io)
        assertSame(provider.io, provider.default)
    }

    @Test
    fun `默认构造自带独立虚拟调度器可直接推进`() {
        val provider = TestDispatcherProvider()
        var executed = false
        kotlinx.coroutines.runBlocking {
            provider.scheduler.advanceUntilIdle()
        }
        // 未排入任务时不抛异常即可
        assertFalse(executed)
    }
}
