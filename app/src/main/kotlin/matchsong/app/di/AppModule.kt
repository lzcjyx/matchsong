package matchsong.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import matchsong.core.audio.android.RecordingFileManager
import matchsong.core.common.time.Clock
import matchsong.data.local.consent.DataStoreConsentRepository
import matchsong.domain.analysis.RecordAnalysisUseCase
import matchsong.domain.port.AnalysisHistoryRepository
import matchsong.domain.port.ConsentRepository
import matchsong.domain.port.FavoritesRepository
import matchsong.domain.port.FeedbackRepository
import matchsong.domain.port.RecordingFileCleaner
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.SongRepository
import matchsong.domain.recommendation.GetFavoriteSongIdsUseCase
import matchsong.domain.recommendation.GetRecommendationsUseCase
import matchsong.domain.recommendation.SubmitFeedbackUseCase
import matchsong.domain.recommendation.ToggleFavoriteUseCase
import matchsong.domain.recording.CleanupStaleRecordingsUseCase
import matchsong.domain.usecase.AcceptConsentUseCase
import matchsong.domain.usecase.DeleteAllDataUseCase
import matchsong.domain.usecase.GetOnboardingStatusUseCase
import java.io.File
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 隐私说明版本常量：与 Onboarding 文案同源（SPEC §10.6）。
 * 文案变更时必须递增此版本，否则已同意用户不会重新看到新说明。
 */
const val PRIVACY_NOTICE_VERSION = "1.0"

/** ConsentRepository 实现选择 key。 */
const val KEY_REAL_CONSENT = "real"

/**
 * M2.3-2 应用级 DI 装配（main）。
 *
 * ConsentRepository 经 Map 多绑定选择：main 提供 [KEY_REAL_CONSENT]（DataStore）；
 * debug source set 的 DebugFakeBindings 提供 "fake" key（M2.4-1）。
 * 选择器按 BuildConfig.DEBUG 取 key（debug 无 fake key 时回退 real）。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @IntoMap
    @StringKey(KEY_REAL_CONSENT)
    @Singleton
    fun provideRealConsentRepository(
        @ApplicationContext context: Context,
    ): ConsentRepository = DataStoreConsentRepository(context)

    @Provides
    @Singleton
    fun provideConsentRepository(
        impls: Map<String, @JvmSuppressWildcards Provider<ConsentRepository>>,
    ): ConsentRepository {
        val key = if (matchsong.app.BuildConfig.DEBUG && impls.containsKey("fake")) "fake" else KEY_REAL_CONSENT
        return impls.getValue(key).get()
    }

    @Provides
    @Singleton
    fun provideAcceptConsentUseCase(repo: ConsentRepository): AcceptConsentUseCase = AcceptConsentUseCase(repo)

    @Provides
    @Singleton
    fun provideGetOnboardingStatusUseCase(repo: ConsentRepository): GetOnboardingStatusUseCase =
        GetOnboardingStatusUseCase(repo, PRIVACY_NOTICE_VERSION)

    /**
     * M3.5-1 录音临时文件清理 Port 绑定（ARCHITECTURE.md §7.3）：
     * 目录 cacheDir/recordings/（缓存目录，系统可清理），实现为
     * RecordingFileManager（纯 java.io，JVM 可测）。
     */
    @Provides
    @Singleton
    fun provideRecordingFileCleaner(
        @ApplicationContext context: Context,
    ): RecordingFileCleaner = RecordingFileManager(File(context.cacheDir, RecordingFileManager.RECORDINGS_DIR_NAME))

    /** M3.5-2 启动过期残留清理用例绑定（FR-REC-8）。 */
    @Provides
    @Singleton
    fun provideCleanupStaleRecordingsUseCase(cleaner: RecordingFileCleaner): CleanupStaleRecordingsUseCase =
        CleanupStaleRecordingsUseCase(cleaner)

    /** M7.6-2 推荐用例绑定（歌曲库 + 设置 → 推荐结果）。 */
    @Provides
    @Singleton
    fun provideGetRecommendationsUseCase(
        songRepo: SongRepository,
        settingsRepo: SettingsRepository,
    ): GetRecommendationsUseCase = GetRecommendationsUseCase(songRepo, settingsRepo)

    /** M8.3-1 收藏用例绑定（详情页/收藏页共用单一写入入口）。 */
    @Provides
    @Singleton
    fun provideToggleFavoriteUseCase(repo: FavoritesRepository): ToggleFavoriteUseCase = ToggleFavoriteUseCase(repo)

    /** M10.6 反馈提交用例绑定（BUG-001：FR-HX-3 UI 接线；仅保存不调权重）。 */
    @Provides
    @Singleton
    fun provideSubmitFeedbackUseCase(repo: FeedbackRepository): SubmitFeedbackUseCase = SubmitFeedbackUseCase(repo)

    @Provides
    @Singleton
    fun provideGetFavoriteSongIdsUseCase(repo: FavoritesRepository): GetFavoriteSongIdsUseCase =
        GetFavoriteSongIdsUseCase(repo)

    /** M8.4-1 记录分析历史用例绑定（FR-HX-1 数据侧，M8.2 分析完成后装配）。 */
    @Provides
    @Singleton
    fun provideRecordAnalysisUseCase(
        historyRepository: AnalysisHistoryRepository,
        clock: Clock,
    ): RecordAnalysisUseCase = RecordAnalysisUseCase(historyRepository, clock)

    /**
     * M9.3 删除全部数据用例绑定（FR-PRIV-5/ACC-15）：
     * 清空历史/收藏/反馈/设置/同意/录音缓存，恢复首次启动状态。
     */
    @Provides
    @Singleton
    fun provideDeleteAllDataUseCase(
        historyRepository: AnalysisHistoryRepository,
        favoritesRepository: FavoritesRepository,
        feedbackRepository: FeedbackRepository,
        settingsRepository: SettingsRepository,
        consentRepository: ConsentRepository,
        fileCleaner: RecordingFileCleaner,
    ): DeleteAllDataUseCase =
        DeleteAllDataUseCase(
            historyRepository = historyRepository,
            favoritesRepository = favoritesRepository,
            feedbackRepository = feedbackRepository,
            settingsRepository = settingsRepository,
            consentRepository = consentRepository,
            fileCleaner = fileCleaner,
        )
}
