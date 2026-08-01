package matchsong.core.audio.analysis

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import matchsong.core.audio.algorithm.PitchPostProcessor
import matchsong.core.audio.algorithm.QualityAnalyzer
import matchsong.core.audio.algorithm.YinPitchDetector
import matchsong.core.audio.api.AudioFrameSource
import matchsong.core.audio.api.PitchTracker
import matchsong.domain.analysis.AnalysisConfig
import matchsong.domain.analysis.AnalysisWarning
import matchsong.domain.analysis.ComfortRangeEstimator
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.analysis.PitchAnalysisFrame
import matchsong.domain.analysis.PitchAnalysisTrack
import matchsong.domain.analysis.PitchStabilityMetrics
import matchsong.domain.analysis.VocalRangeEstimator
import matchsong.domain.analysis.VoiceAnalysisResult

/**
 * M5.6-1 录音分析用例（ARCHITECTURE.md §9.1 流水线编排）。
 *
 * 流水线：质量门禁（不合格短路，P6）→ YIN 音高检测 → 后处理 → 音域/舒适区/稳定性统计 → 组装。
 * 可取消（suspend + ensureActive）；置信度分档（SPEC §13）；算法版本随结果。
 *
 * 位置说明：编排放 core:audio（依赖 domain 统计，无循环；domain 保持纯 Kotlin 低层）。
 */
class AnalyzeRecordingUseCase(
    private val qualityAnalyzer: QualityAnalyzer = QualityAnalyzer(),
    private val pitchTracker: PitchTracker = YinPitchDetector(),
    private val postProcessor: PitchPostProcessor = PitchPostProcessor(),
) {
    @Suppress("LongMethod") // 流水线编排（质量→YIN→后处理→统计→组装），顺序清晰优于拆散
    suspend operator fun invoke(source: AudioFrameSource): VoiceAnalysisResult {
        currentCoroutineContext().ensureActive()

        // 1. 质量门禁（P6：不可靠音频不得产生正式结果）
        val quality = qualityAnalyzer.analyze(source)
        if (!quality.isUsable) {
            return VoiceAnalysisResult(
                qualityUsable = false,
                qualityWarnings = quality.warnings.map { it.name },
                vocalRange = null,
                comfortRange = null,
                stability = null,
                voicedFrameCount = 0,
                totalFrameCount = quality.analyzableFrameCount,
                confidenceLevel = ConfidenceLevel.LOW,
                warnings =
                    quality.warnings.map { warn ->
                        when (warn) {
                            matchsong.core.audio.algorithm.QualityWarning.TOO_SHORT ->
                                AnalysisWarning.INSUFFICIENT_SAMPLES
                            matchsong.core.audio.algorithm.QualityWarning.SILENT,
                            matchsong.core.audio.algorithm.QualityWarning.TOO_QUIET,
                            matchsong.core.audio.algorithm.QualityWarning.NOISY,
                            matchsong.core.audio.algorithm.QualityWarning.CLIPPING,
                            matchsong.core.audio.algorithm.QualityWarning.INSUFFICIENT_VOICE,
                            -> AnalysisWarning.INSUFFICIENT_SAMPLES
                        }
                    },
                algorithmVersion = ALGORITHM_VERSION,
            )
        }

        // 2. YIN 音高检测 + 后处理
        currentCoroutineContext().ensureActive()
        val frames = source.readFrames()
        val pitchTrack = pitchTracker.track(frames)
        val processed = postProcessor.process(pitchTrack)

        // 3. 映射为领域输入
        val analysisTrack =
            PitchAnalysisTrack(
                frames =
                    processed.frames.map {
                        PitchAnalysisFrame(
                            timestampMs = it.timestampMs,
                            midiNote = it.midiNote,
                            isVoiced = it.isVoiced,
                        )
                    },
            )

        // 4. 统计
        currentCoroutineContext().ensureActive()
        val voicedMidis = analysisTrack.voicedFrames.map { it.midiNote }
        val vocalRange = VocalRangeEstimator.estimate(analysisTrack)
        val low = vocalRange.stableLowestMidi
        val high = vocalRange.stableHighestMidi
        val stableRange: Pair<Double, Double>? =
            if (vocalRange.sampleSufficiency && low != null && high != null) {
                low to high
            } else {
                null
            }
        val comfortRange = ComfortRangeEstimator.estimate(voicedMidis, stableRange)
        val stability = PitchStabilityMetrics.compute(analysisTrack)

        // BUG-015 语音干扰门禁：稳定片段比例过低 → 判定录音以非歌唱语音为主
        // （说话语调连续滑动，稳定帧占比远低于演唱），按「有效演唱片段不足」处理：
        // 音域/舒适区置空 + LOW 置信度（ACC-9 不生成正式推荐），提示用户重录。
        val speechDominant = stability.stableFrameRatio < AnalysisConfig.MIN_STABLE_FRAME_RATIO

        // 5. 置信度分档（SPEC §13）
        val rangeConfidence = vocalRange.confidence
        val confidenceLevel =
            when {
                speechDominant -> ConfidenceLevel.LOW
                rangeConfidence >= 0.7 -> ConfidenceLevel.HIGH
                rangeConfidence >= 0.5 -> ConfidenceLevel.MEDIUM
                else -> ConfidenceLevel.LOW
            }

        // 6. 警告
        val warnings =
            buildList {
                if (speechDominant) add(AnalysisWarning.INSUFFICIENT_SAMPLES)
                if (!vocalRange.sampleSufficiency) add(AnalysisWarning.INSUFFICIENT_SAMPLES)
                if (vocalRange.warning == AnalysisWarning.LOW_CONFIDENCE) add(AnalysisWarning.LOW_CONFIDENCE)
            }

        return VoiceAnalysisResult(
            qualityUsable = true,
            qualityWarnings = emptyList(),
            vocalRange = if (vocalRange.sampleSufficiency && !speechDominant) vocalRange else null,
            comfortRange = if (speechDominant) null else comfortRange,
            stability = stability,
            voicedFrameCount = analysisTrack.voicedFrameCount,
            totalFrameCount = frames.size,
            confidenceLevel = confidenceLevel,
            warnings = warnings,
            algorithmVersion = ALGORITHM_VERSION,
        )
    }

    companion object {
        const val ALGORITHM_VERSION = "1.0.0"
    }
}
