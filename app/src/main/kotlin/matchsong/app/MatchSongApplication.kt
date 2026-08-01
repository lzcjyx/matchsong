package matchsong.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import matchsong.core.common.log.Logger
import matchsong.domain.recording.CleanupStaleRecordingsUseCase
import javax.inject.Inject

/**
 * M1.1-2 应用入口。Hilt 装配根。
 */
@HiltAndroidApp
class MatchSongApplication : Application() {
    /** M3.5-2 过期录音残留清理用例（FR-REC-8，ARCHITECTURE.md §7.3/§11）。 */
    @Inject
    lateinit var cleanupStaleRecordingsUseCase: CleanupStaleRecordingsUseCase

    /** M1.4-3 统一日志（Release 自动脱敏 FR-PRIV-4，M9.4 接入应用入口日志）。 */
    @Inject
    lateinit var logger: Logger

    /** 应用级作用域：随进程存活，仅承载启动清理等一次性后台任务。 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        launchStartupCleanup()
    }

    /**
     * 启动时后台清理过期录音残留（FR-REC-8）：Dispatchers.Default 上执行，
     * 失败仅记录日志、不阻塞启动；日志只含删除数量、不含文件路径（FR-PRIV-4）。
     */
    private fun launchStartupCleanup() {
        appScope.launch {
            try {
                val deleted = cleanupStaleRecordingsUseCase()
                logger.d(TAG, "启动清理过期录音残留：删除 $deleted 个文件")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(TAG, "启动清理过期录音残留失败", e)
            }
        }
    }

    private companion object {
        const val TAG = "MatchSongApplication"
    }
}
