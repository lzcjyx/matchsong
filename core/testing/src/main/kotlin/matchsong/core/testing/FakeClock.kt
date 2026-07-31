package matchsong.core.testing

import matchsong.core.common.time.Clock

/**
 * 可推进的虚拟时钟（ARCHITECTURE.md §16.1，M1.4-4）。
 *
 * 确定性时间测试用：状态机倒计时、节流、耗时测量注入本实现，
 * 通过 [advanceBy] 推进时间；[nowNanos] 由毫秒派生（1ms = 1_000_000ns），
 * 保证同一时钟内毫秒/纳秒读数一致。不提供回拨（真实时钟不回拨）。
 */
class FakeClock(
    initialMillis: Long = 0L,
) : Clock {
    private var currentMillis: Long = initialMillis

    override fun nowMillis(): Long = currentMillis

    override fun nowNanos(): Long = currentMillis * NANOS_PER_MILLI

    /** 推进虚拟时间；[durationMs] 必须非负。 */
    fun advanceBy(durationMs: Long): FakeClock {
        require(durationMs >= 0L) { "advanceBy 不支持回拨：durationMs=$durationMs" }
        currentMillis += durationMs
        return this
    }

    private companion object {
        const val NANOS_PER_MILLI: Long = 1_000_000L
    }
}
