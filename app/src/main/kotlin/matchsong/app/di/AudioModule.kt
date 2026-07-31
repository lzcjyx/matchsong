package matchsong.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import matchsong.core.audio.android.AndroidAudioRecorder
import matchsong.core.audio.android.AndroidRecordingPort
import matchsong.core.audio.android.RecordingSessionRunner
import matchsong.core.audio.api.AudioRecorder
import matchsong.domain.recording.PermissionStateMachine
import matchsong.domain.recording.RecordingPort
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
    fun provideAudioRecorder(): AudioRecorder = AndroidAudioRecorder()

    @Provides
    @Singleton
    fun provideRecordingSessionRunner(recorder: AudioRecorder): RecordingSessionRunner =
        RecordingSessionRunner(recorder).also { RecordingSessionRunner.instance = it }

    @Provides
    @Singleton
    fun provideRecordingPort(
        @ApplicationContext context: Context,
        runner: RecordingSessionRunner,
    ): RecordingPort = AndroidRecordingPort(context, runner)

    @Provides
    fun providePermissionStateMachine(): PermissionStateMachine = PermissionStateMachine()
}
