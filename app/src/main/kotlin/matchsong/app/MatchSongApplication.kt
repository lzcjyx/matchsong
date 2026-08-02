package matchsong.app

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import matchsong.core.common.log.Logger
import matchsong.data.local.repository.SongImportRepository
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

    /** BUG-016：内置曲库导入仓库（FR-SONG-4；数据源 assets/songs/mvp-songs.json）。 */
    @Inject
    lateinit var songImportRepository: SongImportRepository

    /** BUG-018：曲库计数（启动导入仅空库执行——用户已导入歌曲包时不覆盖）。 */
    @Inject
    lateinit var songDao: matchsong.data.local.db.dao.SongDao

    /** assets 读取（内置曲库）。 */
    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    /** 应用级作用域：随进程存活，仅承载启动清理/曲库装载等一次性后台任务。 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        launchStartupCleanup()
        ensureSongCatalog()
    }

    /**
     * BUG-016 修复：内置曲库启动装载（幂等，FR-SONG-4）。
     *
     * 读取 assets/songs/mvp-songs.json → [SongImportRepository.import]（版本比对幂等，
     * 同版本跳过、差异事务替换）；失败仅记录日志不阻塞启动（推荐页以空态降级，可重试）。
     * 日志只含数量与结果类型，不含歌曲内容（FR-PRIV-4）。
     */
    private fun ensureSongCatalog() {
        appScope.launch {
            try {
                // 仅空库或旧内置版本（1.0.0）时导入——BUG-018 后用户下载过歌曲包
                // （版本差异）则尊重用户曲库；BUG-023 数据集扩至 56 首（版本 1.1.0）
                // 时旧内置库需可升级
                val storedVersions = songDao.getDataVersions().toSet()
                val needsImport =
                    songDao.count() == 0 || storedVersions == setOf(OLD_BUILTIN_DATA_VERSION)
                if (!needsImport) {
                    logger.d(TAG, "曲库非空且非旧内置版本，跳过内置导入")
                    return@launch
                }
                val json =
                    appContext.assets
                        .open("songs/mvp-songs.json")
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                songImportRepository.import(json).fold(
                    onSuccess = { outcome ->
                        logger.i(
                            TAG,
                            "歌曲目录就绪：${outcome.importedCount} 首（替换=${outcome.replaced}）",
                        )
                    },
                    onFailure = { e -> logger.e(TAG, "歌曲目录导入失败（安全错误）", e) },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(TAG, "歌曲目录加载失败", e)
            }
        }
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

        /** 旧内置曲库版本（BUG-023 升级判定：仅该版本可被内置新版本替换）。 */
        const val OLD_BUILTIN_DATA_VERSION = "1.0.0"
    }
}
