package matchsong.app.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import matchsong.core.common.dispatcher.DispatcherProvider

/**
 * Android 生产调度器实现（ARCHITECTURE.md §14.1，M1.4-2）。
 *
 * [main] 使用 [Dispatchers.Main]（依赖 kotlinx-coroutines-android，仅 app 层可提供）；
 * [io]/[default] 使用全局 [Dispatchers.IO]/[Dispatchers.Default]。
 * 经 CoreModule 以单例绑定注入。
 */
object AndroidDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main

    override val io: CoroutineDispatcher = Dispatchers.IO

    override val default: CoroutineDispatcher = Dispatchers.Default
}
