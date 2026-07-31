package matchsong.core.common.dispatcher

import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * DispatcherProvider 测试（M1.4-2 验收：生产实现返回正确调度器）。
 */
class DispatcherProviderTest {
    @Test
    fun `io 绑定全局 Dispatchers_IO`() {
        assertEquals(Dispatchers.IO, DefaultDispatcherProvider.io)
    }

    @Test
    fun `default 绑定全局 Dispatchers_Default`() {
        assertEquals(Dispatchers.Default, DefaultDispatcherProvider.default)
    }

    @Test
    fun `main 在纯 JVM 环境不可用且抛出明确异常`() {
        val exception =
            assertThrows(UnsupportedOperationException::class.java) {
                DefaultDispatcherProvider.main
            }
        assertEquals(true, exception.message?.contains("Dispatchers.Main") == true)
    }
}
