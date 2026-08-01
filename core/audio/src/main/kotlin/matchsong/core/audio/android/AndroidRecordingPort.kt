package matchsong.core.audio.android

import android.content.Context
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import matchsong.domain.recording.RecordingPort
import matchsong.domain.recording.RecordingState
import matchsong.domain.recording.VolumeLevel

/**
 * M3.2-2 AndroidRecordingPort：UI ↔ 录音系统的通信桥（ARCHITECTURE.md §8.3）。
 *
 * - startRecording：启动前台服务（startForegroundService）→ 服务触发会话；
 * - 状态流/音量流：转发 [RecordingSessionRunner] 的流（服务与 UI 共享实例）；
 * - 前后台切换：服务独立于 Activity 继续录音（ACC-5）；
 * - Activity 重建：重新获取 runner 实例，流自动恢复。
 */
class AndroidRecordingPort(
    private val context: Context,
    private val runner: RecordingSessionRunner,
) : RecordingPort {
    override fun startRecording() {
        RecordingService.start(context)
    }

    override fun stopRecording() {
        RecordingService.stop(context, interrupted = false)
    }

    override val stateFlow: StateFlow<RecordingState> get() = runner.stateFlow

    override val volumeFlow: SharedFlow<VolumeLevel> get() = runner.volumeFlow

    /** BUG-013：倒计时剩余秒数转发（3→2→1）。 */
    override val countdownSeconds: StateFlow<Int> get() = runner.countdownSeconds
}
