package matchsong.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import matchsong.core.audio.algorithm.QualityAnalyzer
import matchsong.core.audio.analysis.AnalyzeRecordingUseCase
import matchsong.core.audio.android.AndroidAudioRecorder
import matchsong.core.audio.android.AndroidRecordingPort
import matchsong.core.audio.android.RecordingFileManager
import matchsong.core.audio.android.RecordingSessionRunner
import matchsong.core.audio.api.AudioRecorder
import matchsong.core.common.log.Logger
import matchsong.domain.recording.PermissionStateMachine
import matchsong.domain.recording.RecordingPort
import java.io.File
import javax.inject.Singleton

/**
 * M3.2 音频装配（ARCHITECTURE.md §15 AudioModule）。
 *
 * - AudioRecorder → AndroidAudioRecorder（M3.3-2，VOICE_RECOGNITION/44.1k/16bit/mono + 降级链）；
 * - RecordingSessionRunner 单例：服务与 UI 共享（RecordingService 经静态引用访问）；
 * - RecordingPort → AndroidRecordingPort（UI 通信桥，M3.2-2）；
 * - PermissionStateMachine 每次注入新实例（状态不持久化，会话重建，ARCHITECTURE.md §6.2）。
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {
    @Provides
    @Singleton
    fun provideAudioRecorder(logger: Logger): AudioRecorder = AndroidAudioRecorder(logger = logger)

    @Provides
    @Singleton
    fun provideRecordingSessionRunner(
        recorder: AudioRecorder,
        fileManager: RecordingFileManager,
        logger: Logger,
    ): RecordingSessionRunner =
        RecordingSessionRunner(recorder, fileManager, logger).also { RecordingSessionRunner.instance = it }

    @Provides
    @Singleton
    fun provideRecordingPort(
        @ApplicationContext context: Context,
        runner: RecordingSessionRunner,
    ): RecordingPort = AndroidRecordingPort(context, runner)

    @Provides
    fun providePermissionStateMachine(): PermissionStateMachine = PermissionStateMachine()

    /** M8.1-1 质量检测器（QualityResultViewModel 消费）。 */
    @Provides
    @Singleton
    fun provideQualityAnalyzer(): QualityAnalyzer = QualityAnalyzer()

    /** M8.1-1 分析用例（AnalyzingViewModel 消费）。 */
    @Provides
    @Singleton
    fun provideAnalyzeRecordingUseCase(): AnalyzeRecordingUseCase = AnalyzeRecordingUseCase()

    /** 录音文件管理（M8.1-1：runner 落盘 + Cleanup 实现共用实例）。 */
    @Provides
    @Singleton
    fun provideRecordingFileManager(
        @ApplicationContext context: Context,
    ): RecordingFileManager = RecordingFileManager(File(context.cacheDir, RecordingFileManager.RECORDINGS_DIR_NAME))
}
