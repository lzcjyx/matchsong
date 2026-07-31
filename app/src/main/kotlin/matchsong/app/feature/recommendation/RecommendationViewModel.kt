package matchsong.app.feature.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.domain.analysis.VoiceAnalysisResult
import matchsong.domain.recommendation.GetRecommendationsUseCase
import matchsong.domain.recommendation.RecommendationResult
import javax.inject.Inject

/**
 * M7.6-2 推荐 ViewModel：加载 → 成功/空/失败（M2.2-3 状态组件）。
 */
@HiltViewModel
class RecommendationViewModel
    @Inject
    constructor(
        private val getRecommendations: GetRecommendationsUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UiState>(UiState.Idle)
        val state: StateFlow<UiState> = _state.asStateFlow()

        sealed interface UiState {
            data object Idle : UiState

            data object Loading : UiState

            data class Success(val result: RecommendationResult) : UiState

            data class Error(val message: String) : UiState
        }

        fun load(analysis: VoiceAnalysisResult) {
            _state.value = UiState.Loading
            viewModelScope.launch {
                try {
                    val result = getRecommendations(analysis)
                    _state.value = UiState.Success(result)
                } catch (e: Exception) {
                    _state.value = UiState.Error("推荐加载失败：${e.message}")
                }
            }
        }
    }
