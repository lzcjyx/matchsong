package matchsong.core.audio.android

import android.util.Log
import matchsong.core.common.log.LogRedactor
import matchsong.core.common.log.Logger

/**
 * core:audio 内部默认 [Logger] 实现（ARCHITECTURE.md §13，FR-PRIV-4）：android.util.Log 封装。
 *
 * app 层经 Hilt 注入全局 Logger 时，可向 [AndroidAudioRecorder] 等构造传入统一实现；
 * 未注入时使用本默认实现，保证 P9「每个 catch 必须记录日志」在模块内始终成立。
 *
 * 脱敏（M9.4）：所有消息与堆栈输出统一经 [LogRedactor] 过滤（路径/UUID/设备标识）。
 * 注意：不直接向 Log 传 Throwable——android.util.Log 会打印原始异常链（含未脱敏 message），
 * 因此将堆栈序列化后整体脱敏再作为消息输出（Release 不含敏感信息，Debug 保留可读性）。
 */
internal class AndroidLogLogger : Logger {
    override fun d(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(Log.DEBUG, tag, message, throwable)
    }

    override fun i(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(Log.INFO, tag, message, throwable)
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(Log.WARN, tag, message, throwable)
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        log(Log.ERROR, tag, message, throwable)
    }

    private fun log(
        level: Int,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val safeMessage = LogRedactor.redact(message)
        val full =
            if (throwable != null) {
                "$safeMessage\n${LogRedactor.redact(throwable.stackTraceToString())}"
            } else {
                safeMessage
            }
        when (level) {
            Log.DEBUG -> Log.d(tag, full)
            Log.INFO -> Log.i(tag, full)
            Log.WARN -> Log.w(tag, full)
            else -> Log.e(tag, full)
        }
    }
}
