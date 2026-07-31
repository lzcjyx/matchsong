package matchsong.app.feature.analyzing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.core.audio.analysis.AnalyzeRecordingUseCase
import matchsong.core.audio.api.WavFileSource
import matchsong.domain.analysis.VoiceAnalysisResult
import java.io.File
import javax.inject.Inject

/**
 * M8.1-1 分析中 ViewModel：执行完整分析管线（质量门禁 → YIN → 后处理 → 统计）。
 */
@HiltViewModel
class AnalyzingViewModel
    @Inject
    constructor(
        private val analyzeUseCase: AnalyzeRecordingUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UiState>(UiState.Idle)
        val state: StateFlow<UiState> = _state.asStateFlow()

        sealed interface UiState {
            data object Idle : UiState

            data object Analyzing : UiState

            data class Done(val result: VoiceAnalysisResult) : UiState

            data class Error(val message: String) : UiState
        }

        /** 执行分析（质量不合格 → 返回质量失败结果，不生成正式结果，P6）。 */
        fun analyze(wavFile: File?) {
            if (wavFile == null || !wavFile.exists()) {
                _state.value = UiState.Error("录音文件不可用，请重新录制")
                return
            }
            _state.value = UiState.Analyzing
            viewModelScope.launch {
                try {
                    val result = analyzeUseCase(WavFileSource(wavFile))
                    _state.value = UiState.Done(result)
                } catch (e: Exception) {
                    _state.value = UiState.Error("分析失败：${e.message}")
                }
            }
        }
    }
