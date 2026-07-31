package matchsong.core.common.time

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Clock 测试（M1.4-2 验收：Clock 单调递增、贴近系统时间）。
 */
class ClockTest {
    @Test
    fun `nowMillis 位于调用前后系统墙钟之间`() {
        val before = System.currentTimeMillis()
        val now = SystemClock.nowMillis()
        val after = System.currentTimeMillis()
        assertTrue(now in before..after, "nowMillis 应贴近系统墙钟")
    }

    @Test
    fun `nowNanos 单调递增`() {
        val first = SystemClock.nowNanos()
        Thread.sleep(2)
        val second = SystemClock.nowNanos()
        assertTrue(second > first, "nowNanos 应单调递增")
    }
}
