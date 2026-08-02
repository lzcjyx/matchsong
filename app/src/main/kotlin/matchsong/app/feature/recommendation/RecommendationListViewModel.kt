package matchsong.app.feature.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.domain.analysis.RecordAnalysisUseCase
import matchsong.domain.analysis.VoiceAnalysisResult
import matchsong.domain.recommendation.GetRecommendationsUseCase
import matchsong.domain.recommendation.RecommendationRefs
import matchsong.domain.recommendation.RecommendationResult
import javax.inject.Inject

/**
 * M8.2-1 推荐列表 ViewModel：用分析结果执行真实推荐（M7 引擎 + 歌曲库）。
 */
@HiltViewModel
class RecommendationListViewModel
    @Inject
    constructor(
        private val getRecommendations: GetRecommendationsUseCase,
        private val recordAnalysis: RecordAnalysisUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UiState>(UiState.Idle)
        val state: StateFlow<UiState> = _state.asStateFlow()

        sealed interface UiState {
            data object Idle : UiState

            data object Loading : UiState

            data class Success(val result: RecommendationResult) : UiState

            data class Error(val message: String) : UiState
        }

        fun load(analysis: VoiceAnalysisResult?) {
            if (analysis == null) {
                _state.value = UiState.Error("缺少分析结果，请先完成声音分析")
                return
            }
            _state.value = UiState.Loading
            viewModelScope.launch {
                try {
                    val result = getRecommendations(analysis)
                    // BUG-022：历史记录保存（FR-HX-1）——此前 RecordAnalysisUseCase 零调用方，
                    // 历史页永远为空；无论推荐是否为空都记录本次分析摘要
                    recordAnalysis(
                        result = analysis,
                        recommendationRefs =
                            RecommendationRefs(
                                songIds = result.recommendations.map { it.song.songId },
                                weightsVersion = result.weightsVersion,
                            ),
                    )
                    _state.value = UiState.Success(result)
                } catch (e: Exception) {
                    _state.value = UiState.Error("推荐加载失败：${e.message}")
                }
            }
        }
    }
