package matchsong.domain.recommendation

import matchsong.domain.analysis.VoiceAnalysisResult
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.SongRepository

/**
 * M7.6-2 获取推荐用例（端到端装配：用户设置 → 歌曲库 → 推荐结果）。
 */
class GetRecommendationsUseCase(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val engine: RecommendationEngine = RecommendationEngine(),
) {
    suspend operator fun invoke(analysis: VoiceAnalysisResult): RecommendationResult {
        val settings = settingsRepository.getSettings()
        val songs = songRepository.getAllMetadata()
        return engine.recommend(analysis, songs, settings)
    }
}
