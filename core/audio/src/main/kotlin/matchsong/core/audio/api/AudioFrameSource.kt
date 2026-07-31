package matchsong.core.audio.api

import matchsong.core.audio.algorithm.Frame

/**
 * M4.1-2 统一帧源接口（FR-QUAL-4：实时 PCM / WAV / Fake Frame Source 可替换）。
 *
 * 质量检测与分析管线消费本接口，不关心帧来自何处。
 */
interface AudioFrameSource {
    /** 产出全部帧（帧参数按 AudioFramePipeline 约定）。 */
    fun readFrames(): List<Frame>
}
