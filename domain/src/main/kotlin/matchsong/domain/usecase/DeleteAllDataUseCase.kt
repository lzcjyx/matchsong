package matchsong.domain.usecase

import matchsong.core.common.error.AppError
import matchsong.core.common.result.OperationResult
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.ConsentRepository
import matchsong.domain.port.FavoritesRepository
import matchsong.domain.port.FeedbackRepository
import matchsong.domain.port.RecordingFileCleaner
import matchsong.domain.port.SettingsRepository

/**
 * M9.3 删除全部数据用例（FR-HX-4 / FR-PRIV-5 / ACC-15，ARCHITECTURE.md §7.4）。
 *
 * 清空全部用户数据并恢复首次启动状态：
 * - Room：分析历史、收藏、反馈；
 * - DataStore：设置（语言/偏好/排除风格）与 Onboarding 标记；
 * - 隐私同意记录（[ConsentRepository.revoke]）——未同意 → 启动分流重新展示 Onboarding；
 * - 录音缓存目录（.pcm/.wav 临时文件，[RecordingFileCleaner.clearAll]）。
 *
 * 歌曲库为应用内置数据（非用户数据，data-model.md §2.8 保留时间=应用生命周期），不删除。
 *
 * 错误语义（P9）：**全部步骤尽力执行**（部分删除优于拒绝删除——隐私最小化），
 * 任一步失败记录首个 [AppError] 并继续；全部成功返回 [OperationResult.Success]。
 */
class DeleteAllDataUseCase(
    private val historyRepository: AnalysisHistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val feedbackRepository: FeedbackRepository,
    private val settingsRepository: SettingsRepository,
    private val consentRepository: ConsentRepository,
    private val fileCleaner: RecordingFileCleaner,
) {
    suspend operator fun invoke(): OperationResult<Unit> {
        var firstFailure: AppError? = null

        suspend fun attempt(
            block: suspend () -> Unit,
            mapError: (Throwable) -> AppError,
        ) {
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (firstFailure == null) firstFailure = mapError(e)
            }
        }

        attempt({ historyRepository.clear() }, { AppError.DatabaseError.Query(it) })
        attempt({ favoritesRepository.clear() }, { AppError.DatabaseError.Query(it) })
        attempt({ feedbackRepository.clear() }, { AppError.DatabaseError.Query(it) })
        attempt({ settingsRepository.clear() }, { AppError.DatabaseError.Query(it) })
        attempt({ consentRepository.revoke() }, { AppError.DatabaseError.Query(it) })
        // 文件清理为尽力而为（Port 契约返回计数，不抛异常），失败计入安全错误日志由调用方处理
        attempt({ fileCleaner.clearAll() }, { AppError.StorageError.Io(it) })

        return if (firstFailure == null) {
            OperationResult.Success(Unit)
        } else {
            OperationResult.Failure(firstFailure)
        }
    }
}
