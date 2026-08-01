package matchsong.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.app.feature.common.ErrorRecoveryHandler
import matchsong.core.common.error.AppError
import matchsong.core.common.result.OperationResult
import matchsong.data.local.network.SongPackImporter
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.FavoritesRepository
import matchsong.domain.port.RecordingFileCleaner
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.SongRepository
import matchsong.domain.usecase.DeleteAllDataUseCase
import javax.inject.Inject

/**
 * M9.3 设置页 ViewModel（FR-HX-4 / FR-PRIV-5 / ACC-15）。
 *
 * 提供全部删除操作：
 * - 粒度删除（历史/收藏/设置/缓存音频）→ 直接调 Port 清空，UI 反馈结果；
 * - 重置应用（[resetAll]）→ [DeleteAllDataUseCase] 全量清空（Room+DataStore+缓存+同意），
 *   成功后进入 [UiState.ResetCompleted]，导航层据此回到首次启动（Splash → Onboarding）。
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val deleteAllDataUseCase: DeleteAllDataUseCase,
        private val historyRepository: AnalysisHistoryRepository,
        private val favoritesRepository: FavoritesRepository,
        private val settingsRepository: SettingsRepository,
        private val fileCleaner: RecordingFileCleaner,
        private val songPackImporter: SongPackImporter,
        private val songRepository: SongRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UiState>(UiState.Idle)
        val state: StateFlow<UiState> = _state.asStateFlow()

        /** 删除操作目标（区分反馈文案与 Busy 状态）。 */
        enum class Action {
            CLEAR_HISTORY,
            CLEAR_FAVORITES,
            CLEAR_SETTINGS,
            CLEAR_CACHE,
            RESET_ALL,
            IMPORT_PACK,
        }

        sealed interface UiState {
            data object Idle : UiState

            data class Busy(val action: Action) : UiState

            data class Done(val action: Action) : UiState

            data class Error(
                val action: Action,
                val message: String,
            ) : UiState

            /** 重置完成：数据已全清，导航层跳转 Splash 恢复首次启动（ACC-15）。 */
            data object ResetCompleted : UiState
        }

        /** BUG-018：当前曲库歌曲数（设置页展示；删除数据后刷新）。 */
        private val _songCount = MutableStateFlow(0)
        val songCount: StateFlow<Int> = _songCount.asStateFlow()

        init {
            refreshSongCount()
        }

        private fun refreshSongCount() {
            viewModelScope.launch {
                _songCount.value = songRepository.getAllMetadata().size
            }
        }

        /** BUG-018：下载并导入歌曲包（替换当前曲库，版本差异事务替换）。 */
        fun importSongPack(packUrl: String) {
            if (packUrl.isBlank() || _state.value is UiState.Busy) return
            viewModelScope.launch {
                _state.value = UiState.Busy(Action.IMPORT_PACK)
                songPackImporter.importPack(packUrl.trim()).fold(
                    onSuccess = { outcome ->
                        _state.value = UiState.Done(Action.IMPORT_PACK)
                        refreshSongCount()
                        _importMessage.value =
                            "导入成功：${outcome.importedCount} 首" +
                            if (outcome.replaced) "（替换原曲库）" else "（已是最新版本）"
                    },
                    onFailure = { e ->
                        _state.value =
                            UiState.Error(
                                Action.IMPORT_PACK,
                                "歌曲包导入失败：${e.message ?: "未知错误"}",
                            )
                    },
                )
            }
        }

        private val _importMessage = MutableStateFlow<String?>(null)
        val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

        fun clearHistory() =
            runAction(Action.CLEAR_HISTORY, { AppError.DatabaseError.Query(it) }) {
                historyRepository.clear()
            }

        fun clearFavorites() =
            runAction(Action.CLEAR_FAVORITES, { AppError.DatabaseError.Query(it) }) {
                favoritesRepository.clear()
            }

        fun clearSettings() =
            runAction(Action.CLEAR_SETTINGS, { AppError.DatabaseError.Query(it) }) {
                settingsRepository.clear()
            }

        fun clearCache() =
            runAction(Action.CLEAR_CACHE, { AppError.StorageError.Io(it) }) {
                fileCleaner.clearAll()
            }

        /** 重置应用：删除全部数据（历史/收藏/反馈/设置/同意/缓存），成功后导航回首次启动。 */
        fun resetAll() {
            viewModelScope.launch {
                _state.value = UiState.Busy(Action.RESET_ALL)
                when (val result = deleteAllDataUseCase()) {
                    is OperationResult.Success -> _state.value = UiState.ResetCompleted
                    is OperationResult.Failure ->
                        _state.value =
                            UiState.Error(Action.RESET_ALL, ErrorRecoveryHandler.userMessage(result.error))
                }
            }
        }

        private fun runAction(
            action: Action,
            mapError: (Throwable) -> AppError,
            block: suspend () -> Unit,
        ) {
            viewModelScope.launch {
                _state.value = UiState.Busy(action)
                try {
                    block()
                    _state.value = UiState.Done(action)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.value = UiState.Error(action, ErrorRecoveryHandler.userMessage(mapError(e)))
                }
            }
        }
    }
