package matchsong.core.audio.android

import android.util.Log
import matchsong.core.common.log.Logger

/**
 * core:audio 内部默认 [Logger] 实现（ARCHITECTURE.md §13）：android.util.Log 封装。
 *
 * app 层经 Hilt 注入全局 Logger 时，可向 [AndroidAudioRecorder] 等构造传入统一实现；
 * 未注入时使用本默认实现，保证 P9「每个 catch 必须记录日志」在模块内始终成立。
 */
internal class AndroidLogLogger : Logger {
    override fun d(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
    }

    override fun i(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
    }
}
