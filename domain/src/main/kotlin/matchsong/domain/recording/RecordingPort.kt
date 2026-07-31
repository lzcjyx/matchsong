package matchsong.domain.recording

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * M3.2-2 录音端口（UI ↔ RecordingService 通信桥，ARCHITECTURE.md §8.3）。
 *
 * feature 层只依赖本接口（P2），实现为 core:audio.android 的 AndroidRecordingPort。
 */
interface RecordingPort {
    /** 开始录音（经前台服务）。 */
    fun startRecording()

    /** 用户提前停止。 */
    fun stopRecording()

    /** 录音状态流（单一事实源）。 */
    val stateFlow: StateFlow<RecordingState>

    /** 实时音量级（≤10Hz 节流，FR-REC-4）。 */
    val volumeFlow: SharedFlow<VolumeLevel>
}

/**
 * M3.6-1 音量级（UI 消费，FR-REC-4）。
 */
data class VolumeLevel(
    /** 当前 RMS（0.0..1.0 归一化）。 */
    val rms: Double,
    /** 是否低于低音量阈值。 */
    val isTooQuiet: Boolean,
    /** 是否削波。 */
    val isClipping: Boolean,
    /** 麦克风是否有输入。 */
    val hasInput: Boolean,
)
