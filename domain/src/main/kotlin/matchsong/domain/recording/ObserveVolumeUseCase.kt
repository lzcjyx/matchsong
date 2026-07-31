package matchsong.domain.recording

import kotlinx.coroutines.flow.Flow

/**
 * M3.6-1 实时音量观察用例（ARCHITECTURE.md §6.1，FR-REC-4）。
 *
 * 音量流已在实现侧按 ≤10Hz 节流（conflate + 100ms，ARCHITECTURE.md §14.2），
 * 本用例为薄封装：UI 层统一经用例取流，不直接触碰 [RecordingPort]。
 */
class ObserveVolumeUseCase(
    private val recordingPort: RecordingPort,
) {
    operator fun invoke(): Flow<VolumeLevel> = recordingPort.volumeFlow
}
