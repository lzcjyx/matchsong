package matchsong.core.common.time

/**
 * 时间抽象（ARCHITECTURE.md §14.1，M1.4-2）。
 *
 * 倒计时、耗时测量均经本接口注入，测试用 core:testing 的 FakeClock 控制虚拟时间。
 */
interface Clock {
    /** 当前时间（epoch 毫秒），用于时间戳/倒计时。 */
    fun nowMillis(): Long

    /** 单调递增纳秒（System.nanoTime 语义），用于耗时测量，不保证与墙钟对应。 */
    fun nowNanos(): Long
}

/** 生产实现：直接委托 [System.currentTimeMillis] / [System.nanoTime]。 */
object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun nowNanos(): Long = System.nanoTime()
}
