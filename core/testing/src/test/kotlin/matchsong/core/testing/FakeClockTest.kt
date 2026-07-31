package matchsong.core.testing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * FakeClock 测试（M1.4-4 验收：推进后 nowMillis/nowNanos 正确）。
 */
class FakeClockTest {
    @Test
    fun `初始时间可配置且可推进`() {
        val clock = FakeClock(initialMillis = 1_000L)
        assertEquals(1_000L, clock.nowMillis())
        clock.advanceBy(500L)
        assertEquals(1_500L, clock.nowMillis())
    }

    @Test
    fun `nowNanos 与毫秒推进一致`() {
        val clock = FakeClock(initialMillis = 1_000L)
        clock.advanceBy(2_000L)
        assertEquals(3_000L * 1_000_000L, clock.nowNanos())
    }

    @Test
    fun `advanceBy 返回自身可链式推进`() {
        val clock = FakeClock()
        clock.advanceBy(100L).advanceBy(250L)
        assertEquals(350L, clock.nowMillis())
    }

    @Test
    fun `负推进抛出异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            FakeClock().advanceBy(-1L)
        }
    }
}
