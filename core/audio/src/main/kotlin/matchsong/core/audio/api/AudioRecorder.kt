package matchsong.core.audio.api

import kotlinx.coroutines.flow.Flow
import matchsong.core.common.result.OperationResult

/**
 * 录音采集接口（ARCHITECTURE.md §8.1/§8.2，ADR-002，M1.4-5）。
 *
 * **冻结契约**：本接口是 core:testing 的 FakeAudioRecorder 与 M3.3-1 的
 * AndroidAudioRecorder 的共同实现目标；任何变更必须与 M3.3-1 评审后同步修改 Fake。
 * [RecordingConfig] 为接口冻结所需的最小字段集，M3.3-1 将按 data-model.md §2.2 细化。
 */
interface AudioRecorder {
    /**
     * 开始录音。
     *
     * @param config 录音配置快照（采样率/位深/声道/最大时长）。
     * @return 成功返回 [OperationResult.Success]；失败（如已启动/设备不可用）返回 [OperationResult.Failure]。
     */
    fun start(config: RecordingConfig): OperationResult<Unit>

    /** 停止录音并释放资源；幂等，未启动时调用为 no-op。 */
    fun stop()

    /**
     * 帧流：采集线程持续产出 [AudioChunk]（含归一化样本与 RMS/峰值聚合指标），带背压。
     * 冷流实现下，collect 结束或协程取消即停止产出。
     */
    val frames: Flow<AudioChunk>
}

/**
 * 录音配置快照（M1.4-5 冻结接口用最小字段集；M3.3-1 将按 data-model.md §2.2 细化为完整字段）。
 * 默认值遵循 ADR-002：44.1kHz / 16bit / mono，最大 30s（FR-REC-2）。
 */
data class RecordingConfig(
    val sampleRateHz: Int = 44_100,
    val channelCount: Int = 1,
    val bitsPerSample: Int = 16,
    val maxDurationMs: Long = 30_000L,
)
