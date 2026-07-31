package matchsong.app.feature.flow

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import matchsong.core.audio.algorithm.AudioQualityReport
import matchsong.domain.analysis.VoiceAnalysisResult
import java.io.File
import javax.inject.Inject

/**
 * M8.1-1 录音→分析流程共享状态（Activity 作用域 ViewModel）。
 *
 * 单一事实源：sessionId / WAV 文件 / 质量报告 / 分析结果 跨页面传递。
 * 各页面 ViewModel 读写本状态（导航仅渲染，ARCHITECTURE.md §17）。
 */
@HiltViewModel
class FlowSessionViewModel
    @Inject
    constructor() : ViewModel() {
        private val _wavFile = MutableStateFlow<File?>(null)
        val wavFile: StateFlow<File?> = _wavFile.asStateFlow()

        private val _qualityReport = MutableStateFlow<AudioQualityReport?>(null)
        val qualityReport: StateFlow<AudioQualityReport?> = _qualityReport.asStateFlow()

        private val _analysisResult = MutableStateFlow<VoiceAnalysisResult?>(null)
        val analysisResult: StateFlow<VoiceAnalysisResult?> = _analysisResult.asStateFlow()

        fun setWavFile(file: File?) {
            _wavFile.value = file
        }

        fun setQualityReport(report: AudioQualityReport?) {
            _qualityReport.value = report
        }

        fun setAnalysisResult(result: VoiceAnalysisResult?) {
            _analysisResult.value = result
        }

        /** 重录：清空会话状态。 */
        fun reset() {
            _wavFile.value = null
            _qualityReport.value = null
            _analysisResult.value = null
        }
    }
