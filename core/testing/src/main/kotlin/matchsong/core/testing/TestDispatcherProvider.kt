package matchsong.core.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import matchsong.core.common.dispatcher.DispatcherProvider

/**
 * 确定性调度器提供者（ARCHITECTURE.md §16.1，M1.4-4）。
 *
 * main/io/default 共享同一个 [StandardTestDispatcher]：任务在虚拟调度器上排队，
 * 由测试显式推进（runTest 的 advanceUntilIdle 等），保证状态机/节流测试确定性。
 *
 * 与 runTest 联用时传入其调度器共享时间轴：
 * `TestDispatcherProvider(StandardTestDispatcher(testScheduler))`。
 */
class TestDispatcherProvider(
    dispatcher: TestDispatcher = StandardTestDispatcher(),
) : DispatcherProvider {
    /** 暴露虚拟调度器（推进任务用）。 */
    val scheduler: TestCoroutineScheduler = dispatcher.scheduler

    override val main: CoroutineDispatcher = dispatcher

    override val io: CoroutineDispatcher = dispatcher

    override val default: CoroutineDispatcher = dispatcher
}
