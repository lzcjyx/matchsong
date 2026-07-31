package matchsong.core.audio.api

/**
 * 一帧音频数据块（ARCHITECTURE.md §8.2，M1.4-5）。
 *
 * [samples] 为归一化样本（-1.0..1.0 的 Float）；
 * [rms]/[peak] 为聚合指标——音量反馈（FR-REC-4）与质量检测（FR-QUAL-1）的输入。
 *
 * 注意：data class 对 [samples]（FloatArray）的 equals 按引用比较，
 * 测试断言请使用 contentEquals。
 */
data class AudioChunk(
    val samples: FloatArray,
    val rms: Double,
    val peak: Double,
)
