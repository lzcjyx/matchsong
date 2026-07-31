package matchsong.app.log

import android.util.Log
import matchsong.app.BuildConfig
import matchsong.core.common.log.LogRedactor
import matchsong.core.common.log.Logger

/**
 * android.util.Log 实现（ARCHITECTURE.md §13，FR-PRIV-4，M1.4-3）。
 *
 * - [d]：调试细节，仅 Debug 构建输出；Release 下 R8 常量折叠 `BuildConfig.DEBUG=false` 移除调用；
 * - [i]/[w]/[e]：Debug 与 Release 均输出（审计事件如同意记录/数据删除、错误堆栈需在 Release 保留），
 *   消息统一经 [LogRedactor] 脱敏（路径/设备标识/会话号）；
 * - 原始音频样本与内容在任何级别禁止输出——调用方约定，本实现不做二进制识别。
 */
class AndroidLogger : Logger {
    override fun d(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, LogRedactor.redact(message), throwable)
        }
    }

    override fun i(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.i(tag, LogRedactor.redact(message), throwable)
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.w(tag, LogRedactor.redact(message), throwable)
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.e(tag, LogRedactor.redact(message), throwable)
    }
}
