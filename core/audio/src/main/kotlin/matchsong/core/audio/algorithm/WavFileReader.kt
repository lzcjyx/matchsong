package matchsong.core.audio.algorithm

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * M3.5-1 WAV 读取器（纯 Kotlin，无 Android 依赖）。
 *
 * 镜像 core:testing 的 WavReader（docs/testing/test-fixture-manifest.md §3.1）：
 * 支持标准 RIFF/WAVE + fmt（audioFormat=1 PCM）+ data 的 16bit 单声道/多声道文件；
 * 非 PCM 格式或缺少必需 chunk 时抛 [IllegalArgumentException]。
 *
 * M4/M5 分析输入：质量检测与音高追踪消费归一化样本（-1.0..1.0，AudioChunk 同格式），
 * 用 [WavData.normalizedSamples] 获取。
 */
class WavFileReader {
    /** WAV 解析结果。 */
    data class WavData(
        val sampleRateHz: Int,
        val channels: Int,
        val bitsPerSample: Int,
        /** 原始 16bit little-endian 样本（多声道为交织排列）。 */
        val samples: ShortArray,
    ) {
        /** 帧数 = 样本数 / 声道数。 */
        val frameCount: Int get() = samples.size / channels

        val durationSec: Double get() = frameCount.toDouble() / sampleRateHz

        /** 归一化样本（-1.0..1.0），与 [matchsong.core.audio.api.AudioChunk.samples] 同格式。 */
        fun normalizedSamples(): FloatArray =
            FloatArray(samples.size) { i ->
                samples[i] / MAX_AMPLITUDE
            }

        private companion object {
            const val MAX_AMPLITUDE: Float = Short.MAX_VALUE.toFloat()
        }
    }

    fun read(file: File): WavData = read(file.readBytes())

    fun read(bytes: ByteArray): WavData {
        require(bytes.size >= HEADER_MIN_BYTES) { "文件过小（${bytes.size}B），不是合法 WAV" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        require(readAscii(buffer, 4) == "RIFF") { "缺少 RIFF 标识" }
        buffer.int // RIFF chunk size
        require(readAscii(buffer, 4) == "WAVE") { "缺少 WAVE 标识" }

        var audioFormat = -1
        var channels = -1
        var sampleRateHz = -1
        var bitsPerSample = -1
        var pcmOffset = -1
        var pcmSize = 0

        while (buffer.remaining() >= 8) {
            val chunkId = readAscii(buffer, 4)
            val chunkSize = buffer.int
            when (chunkId) {
                "fmt " -> {
                    require(chunkSize >= 16) { "fmt chunk 过小：$chunkSize" }
                    audioFormat = buffer.short.toInt() and 0xFFFF
                    channels = buffer.short.toInt() and 0xFFFF
                    sampleRateHz = buffer.int
                    buffer.int // byteRate（可校验，此处忽略）
                    buffer.short // blockAlign
                    bitsPerSample = buffer.short.toInt() and 0xFFFF
                    skip(buffer, chunkSize - 16)
                }
                "data" -> {
                    pcmOffset = buffer.position()
                    pcmSize = chunkSize
                    break
                }
                else -> skip(buffer, chunkSize)
            }
        }

        require(pcmOffset >= 0) { "缺少 data chunk" }
        require(audioFormat == 1) { "仅支持 PCM（audioFormat=1），实际 $audioFormat" }

        val bytesPerSample = bitsPerSample / 8
        val sampleCount = pcmSize / bytesPerSample
        val samples = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            samples[i] = buffer.getShort(pcmOffset + i * bytesPerSample)
        }
        return WavData(sampleRateHz, channels, bitsPerSample, samples)
    }

    private fun readAscii(
        buffer: ByteBuffer,
        length: Int,
    ): String {
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private fun skip(
        buffer: ByteBuffer,
        count: Int,
    ) {
        if (count > 0) buffer.position(buffer.position() + count)
    }

    private companion object {
        const val HEADER_MIN_BYTES = 44
    }
}
