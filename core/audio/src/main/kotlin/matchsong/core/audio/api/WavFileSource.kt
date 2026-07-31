package matchsong.core.audio.api

import matchsong.core.audio.algorithm.AudioFramePipeline
import matchsong.core.audio.algorithm.Frame
import matchsong.core.audio.algorithm.WavFileReader
import java.io.File

/**
 * M4.1-2 WAV 文件帧源：质量/分析主输入（{sessionId}.wav，M3.5-1 格式）。
 */
class WavFileSource(
    private val file: File,
) : AudioFrameSource {
    override fun readFrames(): List<Frame> {
        val wav = WavFileReader().read(file)
        val sampleRate = wav.sampleRateHz
        val samples = wav.normalizedSamples()
        return AudioFramePipeline.process(samples, sampleRate)
    }
}
