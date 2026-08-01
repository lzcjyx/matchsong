package matchsong.core.audio.algorithm

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * M3.5-1 WAV 封装器（纯 Kotlin，无 Android 依赖）。
 *
 * 产出格式与 core:testing 的 [WavTestFileFactory]（docs/testing/test-fixture-manifest.md §3.1）完全一致：
 * 标准 44 字节 RIFF/WAVE 头 —— "RIFF" + chunkSize(36+dataSize) + "WAVE" +
 * "fmt "（PCM=1、声道数、采样率、字节率、块对齐、位深）+ "data" + dataSize + 原始 PCM。
 * 默认参数对应 ADR-002 / data-model.md §2.2：44.1kHz / 16bit / mono。
 *
 * 与测试夹具互操作约定：
 * - 本 writer 产出的文件可被 core:testing WavReader 与 [WavFileReader] 直接读取；
 * - 测试可用 [wrapPcm] 直接获得夹具同构的完整 WAV 字节。
 *
 * 错误语义：IO 失败抛 [IOException]（由调用方映射为 StorageError.Io，P9）；
 * 格式参数非法抛 [IllegalArgumentException]（编程错误，与测试夹具的 require 一致）。
 */
object WavFileWriter {
    /** 标准 RIFF/WAVE 头长度（fmt=16 + data 头，无扩展块）。 */
    const val HEADER_BYTES: Int = 44

    /**
     * 将原始 PCM 文件封装为 WAV 文件。
     *
     * 先读取 PCM 长度构造头（dataSize 直接写入，无需回填），再流式拷贝 PCM，
     * 避免整文件载入内存（30s ≈ 2.65MB，流式拷贝常驻缓冲）。
     *
     * @param pcmFile 原始 PCM 数据（16bit little-endian 交织样本）。
     * @param wavFile 输出 WAV 文件（父目录不存在时自动创建）。
     * @param sampleRateHz 采样率（Hz），必须 > 0。
     * @param channels 声道数，必须 > 0。
     * @param bitsPerSample 位深，必须为 8 的倍数。
     * @return 写入完成的 [wavFile]。
     * @throws IOException PCM 缺失或读写失败。
     * @throws IllegalArgumentException 格式参数非法。
     */
    fun writePcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRateHz: Int,
        channels: Int,
        bitsPerSample: Int,
    ): File {
        validateFormat(sampleRateHz, channels, bitsPerSample)
        val dataSize = pcmFile.length()
        if (!pcmFile.isFile) throw IOException("PCM 文件不存在：${pcmFile.name}")
        if (dataSize > Int.MAX_VALUE) throw IOException("PCM 过大，WAV dataSize 超出 Int 范围：$dataSize 字节")

        wavFile.parentFile?.mkdirs()
        FileOutputStream(wavFile).use { raw ->
            BufferedOutputStream(raw, COPY_BUFFER_SIZE).use { out ->
                out.write(buildHeader(dataSize.toInt(), sampleRateHz, channels, bitsPerSample))
                FileInputStream(pcmFile).use { input ->
                    input.copyTo(out, COPY_BUFFER_SIZE)
                }
            }
        }
        return wavFile
    }

    /**
     * 将内存中的原始 PCM 字节封装为完整 WAV 字节（含头）。
     * 与 [writePcmToWav] 使用同一头部构造逻辑，供测试直接比对夹具格式。
     */
    fun wrapPcm(
        pcm: ByteArray,
        sampleRateHz: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        validateFormat(sampleRateHz, channels, bitsPerSample)
        val header = buildHeader(pcm.size, sampleRateHz, channels, bitsPerSample)
        return header + pcm
    }

    /** 构造标准 44 字节头（小端序，布局与 WavTestFileFactory.wrapInWavHeader 一致）。 */
    private fun buildHeader(
        dataSize: Int,
        sampleRateHz: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val byteRate = sampleRateHz * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteArray(HEADER_BYTES)
        var offset = 0

        fun putAscii(text: String) {
            text.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
            offset += 4
        }

        fun putInt32(value: Int) {
            header[offset++] = (value and 0xFF).toByte()
            header[offset++] = ((value shr 8) and 0xFF).toByte()
            header[offset++] = ((value shr 16) and 0xFF).toByte()
            header[offset++] = ((value shr 24) and 0xFF).toByte()
        }

        fun putInt16(value: Int) {
            header[offset++] = (value and 0xFF).toByte()
            header[offset++] = ((value shr 8) and 0xFF).toByte()
        }

        putAscii("RIFF")
        putInt32(36 + dataSize)
        putAscii("WAVE")
        putAscii("fmt ")
        putInt32(16) // fmt chunk 大小
        putInt16(1) // audioFormat = PCM
        putInt16(channels)
        putInt32(sampleRateHz)
        putInt32(byteRate)
        putInt16(blockAlign)
        putInt16(bitsPerSample)
        putAscii("data")
        putInt32(dataSize)
        check(offset == HEADER_BYTES) { "头部构造错误：offset=$offset 应为 $HEADER_BYTES" }
        return header
    }

    private fun validateFormat(
        sampleRateHz: Int,
        channels: Int,
        bitsPerSample: Int,
    ) {
        require(sampleRateHz > 0 && channels > 0 && bitsPerSample % 8 == 0) {
            "非法格式参数：sampleRateHz=$sampleRateHz channels=$channels bitsPerSample=$bitsPerSample"
        }
    }

    private const val COPY_BUFFER_SIZE: Int = 64 * 1024
}
