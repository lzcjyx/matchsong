package matchsong.core.audio.api

import matchsong.core.audio.algorithm.Frame

/**
 * M5.1-1 音高检测接口（PLAN M5.1，ADR-003）。
 *
 * 独立接口、无 Android UI 依赖、可配置（[PitchTrackerConfig]）、可取消（batch API suspend + isActive）。
 */
interface PitchTracker {
    /** 单帧检测（同步，供逐帧流处理）。 */
    fun detectFrame(frame: Frame): PitchFrame

    /**
     * 批量检测（suspend，可取消）：处理整段帧序列 → [PitchTrack]。
     *
     * 取消语义：协程取消时抛 CancellationException，不产生半成品轨迹。
     */
    suspend fun track(frames: List<Frame>): PitchTrack
}

/**
 * M5.1-1 音高检测配置（ADR-003：65~1046Hz 工作范围，帧 2048@44.1k）。
 */
data class PitchTrackerConfig(
    /** 最低检测频率（C2，ADR-003）。 */
    val minFreqHz: Double = 65.0,
    /** 最高检测频率（C6，ADR-003）。 */
    val maxFreqHz: Double = 1046.0,
    /** YIN 绝对阈值（CMND 谷判定；合成信号 0.10 已够，真实人声需 0.25，M5.8 MIR-1K 标定）。 */
    val threshold: Double = 0.25,
    /** 无音高判定：全局最小 CMND > 该值视为无音高。 */
    val noPitchThreshold: Double = 0.5,
    /** 静音门限：帧 RMS < 该值直接判无音（spike §5.3，与 QualityConfig Q-1 一致）。 */
    val silenceRmsThreshold: Double = 0.01,
    /** 采样率（帧来自 AudioFramePipeline，默认 44.1k）。 */
    val sampleRateHz: Int = 44100,
)

/**
 * M5.1-1 单帧音高结果（data-model §2.4 PitchFrame）。
 */
data class PitchFrame(
    /** 帧时间戳（毫秒，相对录音起点）。 */
    val timestampMs: Long,
    /** 基频（Hz）；无音高时为 NaN。 */
    val f0Hz: Double,
    /** 对应 MIDI 音符（69+12*log2(f/440)）；无音高时为 NaN。 */
    val midiNote: Double,
    /** 置信度（1 - CMND_min，0..1）；无音高为 0。 */
    val confidence: Double,
    /** 帧 RMS（归一化）。 */
    val rms: Double,
    /** 是否判定为有声（有效音高帧）。 */
    val isVoiced: Boolean,
)

/**
 * M5.1-1 音高轨迹（data-model §2.5 PitchTrack）。
 */
data class PitchTrack(
    /** 帧列表（含无效帧，isVoiced=false 标记）。 */
    val frames: List<PitchFrame>,
    /** 有效帧数。 */
    val voicedFrameCount: Int,
    /** 算法版本（随结果落库，FR-HX-1）。 */
    val algorithmVersion: String,
) {
    /** 有效帧（isVoiced=true）列表，按时间序。 */
    val voicedFrames: List<PitchFrame> get() = frames.filter { it.isVoiced }
}
