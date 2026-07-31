package matchsong.domain.analysis

/**
 * M5.3-5 分析输入：领域层音高轨迹视图（ARCHITECTURE.md P3 依赖方向）。
 *
 * 说明：domain 是纯 Kotlin 低层模块，core:audio 依赖 domain（其 android 子包实现
 * domain 端口），domain 反向依赖 core:audio 会构成循环依赖，因此分析输入使用领域
 * 自有模型，由组装层（M5.6/app）将 core:audio 后处理输出
 * （core.audio.api.PitchTrack）映射为 [PitchAnalysisTrack]（与
 * domain.recording.VolumeLevel 由 core:audio 产出的模式一致）。
 *
 * 字段语义与 core.audio.api.PitchFrame 对齐（data-model §2.4/2.5）。
 */
data class PitchAnalysisFrame(
    /** 帧时间戳（毫秒，相对录音起点）。 */
    val timestampMs: Long,
    /** MIDI 音符值（69+12*log2(f/440)）；无声帧为 NaN。 */
    val midiNote: Double,
    /** 是否判定为有声（有效音高帧）。 */
    val isVoiced: Boolean,
)

/**
 * M5.3-5 分析输入：音高轨迹（data-model §2.5 的领域视图）。
 */
data class PitchAnalysisTrack(
    /** 帧列表（含无效帧，isVoiced=false 标记）。 */
    val frames: List<PitchAnalysisFrame>,
) {
    /** 有效帧列表，按时间序。 */
    val voicedFrames: List<PitchAnalysisFrame> get() = frames.filter { it.isVoiced }

    /** 有效帧数。 */
    val voicedFrameCount: Int get() = voicedFrames.size
}
