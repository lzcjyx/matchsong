package matchsong.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.AnalysisSummary
import javax.inject.Inject

/**
 * M8.4-2 历史列表 ViewModel：观察历史摘要流（按时间倒序）+ 单条删除（FR-HX-4）。
 */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val historyRepository: AnalysisHistoryRepository,
    ) : ViewModel() {
        /** 历史列表（按记录时间倒序；空列表 = 空状态）。 */
        val items: StateFlow<List<AnalysisSummary>> =
            historyRepository.observeHistory()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )

        /** 删除单条历史（M9.3 UI 联动；确认弹窗在 Screen 层）。 */
        fun delete(analysisId: String) {
            viewModelScope.launch {
                historyRepository.delete(analysisId)
            }
        }
    }
