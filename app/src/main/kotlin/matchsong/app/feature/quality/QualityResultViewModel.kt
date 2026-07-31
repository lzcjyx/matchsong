package matchsong.app.feature.quality

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import matchsong.core.audio.algorithm.AudioQualityReport
import matchsong.core.audio.algorithm.QualityAnalyzer
import matchsong.core.audio.api.WavFileSource
import java.io.File
import javax.inject.Inject

/**
 * M8.1-1 质量结果 ViewModel：对录音 WAV 执行真实质量检测（FR-QUAL-1/3，ACC-6/7/8）。
 */
@HiltViewModel
class QualityResultViewModel
    @Inject
    constructor(
        private val qualityAnalyzer: QualityAnalyzer,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UiState>(UiState.Idle)
        val state: StateFlow<UiState> = _state.asStateFlow()

        sealed interface UiState {
            data object Idle : UiState

            data object Checking : UiState

            data class Result(val report: AudioQualityReport) : UiState

            data class Error(val message: String) : UiState
        }

        /** 对指定 WAV 文件执行质量检测。 */
        fun analyze(wavFile: File?) {
            if (wavFile == null || !wavFile.exists()) {
                _state.value = UiState.Error("录音文件不可用，请重新录制")
                return
            }
            _state.value = UiState.Checking
            viewModelScope.launch {
                try {
                    val report = qualityAnalyzer.analyze(WavFileSource(wavFile))
                    _state.value = UiState.Result(report)
                } catch (e: Exception) {
                    _state.value = UiState.Error("质量检测失败：${e.message}")
                }
            }
        }
    }
