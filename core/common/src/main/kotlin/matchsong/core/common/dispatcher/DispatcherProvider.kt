package matchsong.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器注入抽象（ARCHITECTURE.md §14.1，M1.4-2）。
 *
 * domain 代码禁止硬编码 [Dispatchers] 全局调度器，一律经本接口注入；
 * 测试用 core:testing 的 TestDispatcherProvider（StandardTestDispatcher）保证确定性。
 */
interface DispatcherProvider {
    /** 仅 UI/ViewModel 使用（Main 线程）。 */
    val main: CoroutineDispatcher

    /** 文件/网络/阻塞 IO。 */
    val io: CoroutineDispatcher

    /** CPU 密集计算（YIN、质量、统计、推荐评分）。 */
    val default: CoroutineDispatcher
}

/**
 * 生产默认实现（ARCHITECTURE.md §14.1）。
 *
 * [io]/[default] 绑定全局 [Dispatchers.IO]/[Dispatchers.Default]；
 * [main] 依赖 kotlinx-coroutines-android（Dispatchers.Main），而 core:common 为纯 Kotlin、
 * 零 Android 依赖（P3），故 [main] 不可用——由 app 层 AndroidDispatcherProvider
 * （Hilt CoreModule 绑定）覆盖提供，测试注入 TestDispatcherProvider。
 */
object DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher
        get() = throw UnsupportedOperationException(
            "Dispatchers.Main 依赖 kotlinx-coroutines-android，core:common 不引入该依赖。" +
                "请使用 app 层 AndroidDispatcherProvider（Hilt 绑定）或测试用 TestDispatcherProvider。",
        )

    override val io: CoroutineDispatcher = Dispatchers.IO

    override val default: CoroutineDispatcher = Dispatchers.Default
}
