package matchsong.domain.recommendation

import matchsong.core.model.song.Credibility
import matchsong.core.model.song.SongMetadata
import matchsong.domain.analysis.AnalysisWarning
import matchsong.domain.analysis.ComfortRangeEstimate
import matchsong.domain.analysis.ConfidenceLevel
import matchsong.domain.analysis.PitchStabilityMetricsResult
import matchsong.domain.analysis.VocalRangeEstimate
import matchsong.domain.analysis.VoiceAnalysisResult

/**
 * M7.6 推荐测试场景工厂（共享 fixture）。
 */
object RecommendationFixtures {
    /** 用户：稳定音域 C3-A4（48-69），舒适区 E3-E4（52-64），高置信。 */
    fun userRange(): VocalRangeEstimate =
        VocalRangeEstimate(
            stableLowestMidi = 48.0,
            stableHighestMidi = 69.0,
            rangeSpanSemitones = 21.0,
            coverage = 0.85,
            confidence = 0.8,
            sampleSufficiency = true,
            warning = AnalysisWarning.NONE,
        )

    fun comfortRange(): ComfortRangeEstimate =
        ComfortRangeEstimate(
            comfortLowestMidi = 52.0,
            comfortHighestMidi = 64.0,
            primaryRangeLowMidi = 52.0,
            primaryRangeHighMidi = 64.0,
            confidence = 0.7,
            sampleSufficiency = true,
            estimateDisclaimer = "本次录音估计",
        )

    fun stability(): PitchStabilityMetricsResult =
        PitchStabilityMetricsResult(
            stableFrameRatio = 0.75,
            pitchDeviationCents = 35.0,
            longNoteDeviationCents = 20.0,
            voicedFrameRatio = 0.8,
        )

    fun analysis(
        confidenceLevel: ConfidenceLevel = ConfidenceLevel.HIGH,
        range: VocalRangeEstimate? = userRange(),
    ): VoiceAnalysisResult =
        VoiceAnalysisResult(
            qualityUsable = true,
            qualityWarnings = emptyList(),
            vocalRange = range,
            comfortRange = if (range != null) comfortRange() else null,
            stability = if (range != null) stability() else null,
            voicedFrameCount = 500,
            totalFrameCount = 650,
            confidenceLevel = confidenceLevel,
            warnings = emptyList(),
            algorithmVersion = "1.0.0",
        )

    /** 歌曲构造器（默认 C3-C5 音域，zh 流行，MEDIUM 可信度）。 */
    fun song(
        songId: String = "s1",
        title: String = "测试歌",
        language: String = "zh",
        genre: String = "流行",
        lowest: Int = 48,
        highest: Int = 72,
        tessLow: Int = 52,
        tessHigh: Int = 64,
        highNoteBurden: Double = 0.3,
        leapDifficulty: Double = 0.3,
        overallDifficulty: Double = 0.4,
        credibility: Credibility = Credibility.MEDIUM,
    ): SongMetadata =
        SongMetadata(
            songId = songId,
            title = title,
            artist = "测试歌手",
            language = language,
            genre = genre,
            originalKeyMidi = 60,
            lowestMidi = lowest,
            highestMidi = highest,
            tessituraLowMidi = tessLow,
            tessituraHighMidi = tessHigh,
            rangeSpanSemitones = highest - lowest,
            highNoteBurden = highNoteBurden,
            longNoteBurden = 0.5,
            leapDifficulty = leapDifficulty,
            rhythmDifficulty = 0.5,
            overallDifficulty = overallDifficulty,
            recommendedKeyShiftMin = -3,
            recommendedKeyShiftMax = 3,
            audioUrl = null,
            dataSource = "test-fixture",
            credibility = credibility,
            dataVersion = "1.0.0",
            importBatchId = "test",
        )
}
