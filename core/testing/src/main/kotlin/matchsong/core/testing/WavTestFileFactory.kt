package matchsong.core.testing

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

/**
 * JVM 内生成标准 WAV 夹具（ARCHITECTURE.md §16.1，M1.4-4）。
 *
 * 格式（docs/testing/test-fixture-manifest.md §3.1）：RIFF/WAVE header + fmt chunk
 * （PCM=1、单声道、44100Hz、字节率 88200、块对齐 2、位深 16）+ data chunk（16bit little-endian PCM）。
 *
 * 与清单夹具对应（§2.1）：
 * - [SignalType.SINE]：FIX-SINE-*（幅值 [amplitude] 的正弦，频率 [frequencyHz]）；
 * - [SignalType.SILENCE]：FIX-SILENCE（数字静音，全零样本）；
 * - [SignalType.NOISE]：FIX-NOISE-WHITE（白噪声，固定种子 [seed] 可复现）；
 * - [SignalType.CLIPPED]：FIX-CLIPPED-440（正弦在 ±[clipLevel] 处硬削波）。
 *
 * 噪声/静音使用固定种子 [seed]，输出可复现（清单"来源=合成脚本，可一键再生成"）。
 */
object WavTestFileFactory {
    /** 信号类型（test-fixture-manifest.md §2.1）。 */
    enum class SignalType { SINE, SILENCE, NOISE, CLIPPED }

    /**
     * 生成完整 WAV 文件字节（含 header）。
     *
     * @param durationSec 时长（秒，> 0）。
     * @param frequencyHz 正弦频率（SINE/CLIPPED 使用）。
     * @param amplitude 幅值（0..1；SINE/NOISE 使用，SILENCE 忽略输出全零）。
     * @param clipLevel 削波限幅（CLIPPED 使用，默认 ±0.3 对应 FIX-CLIPPED-440）。
     */
    fun create(
        durationSec: Double,
        signalType: SignalType = SignalType.SINE,
        frequencyHz: Double = 440.0,
        amplitude: Double = 0.5,
        clipLevel: Double = 0.3,
        sampleRateHz: Int = 44_100,
        channels: Int = 1,
        bitsPerSample: Int = 16,
        seed: Long = 42L,
    ): ByteArray {
        require(durationSec > 0.0) { "durationSec 必须为正，实际 $durationSec" }
        require(sampleRateHz > 0 && channels > 0 && bitsPerSample % 8 == 0) {
            "非法格式参数：sampleRateHz=$sampleRateHz channels=$channels bitsPerSample=$bitsPerSample"
        }
        require(amplitude in 0.0..1.0) { "amplitude 必须在 [0,1]，实际 $amplitude" }

        val bytesPerSample = bitsPerSample / 8
        val sampleCount = (durationSec * sampleRateHz).toInt()
        val random = Random(seed)
        val pcm = ByteArray(sampleCount * channels * bytesPerSample)
        val out = java.nio.ByteBuffer.wrap(pcm).order(java.nio.ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until sampleCount) {
            val normalized: Double =
                when (signalType) {
                    SignalType.SINE -> amplitude * sin(2.0 * PI * frequencyHz * i / sampleRateHz)
                    SignalType.SILENCE -> 0.0
                    SignalType.NOISE -> (random.nextDouble() * 2.0 - 1.0) * amplitude
                    SignalType.CLIPPED -> {
                        val raw = amplitude * sin(2.0 * PI * frequencyHz * i / sampleRateHz)
                        raw.coerceIn(-clipLevel, clipLevel)
                    }
                }
            val intValue =
                (normalized * Short.MAX_VALUE.toDouble()).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            repeat(channels) { out.putShort(intValue.toShort()) }
        }
        return wrapInWavHeader(pcm, sampleRateHz, channels, bitsPerSample)
    }

    /** 生成并写入文件（父目录不存在时自动创建），返回该文件。 */
    fun writeToFile(
        file: File,
        durationSec: Double,
        signalType: SignalType = SignalType.SINE,
        frequencyHz: Double = 440.0,
        amplitude: Double = 0.5,
        clipLevel: Double = 0.3,
        sampleRateHz: Int = 44_100,
        channels: Int = 1,
        bitsPerSample: Int = 16,
        seed: Long = 42L,
    ): File {
        file.parentFile?.mkdirs()
        file.writeBytes(
            create(
                durationSec,
                signalType,
                frequencyHz,
                amplitude,
                clipLevel,
                sampleRateHz,
                channels,
                bitsPerSample,
                seed,
            ),
        )
        return file
    }

    private fun wrapInWavHeader(
        pcm: ByteArray,
        sampleRateHz: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val byteRate = sampleRateHz * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.size

        val header = ByteArrayOutputStream(44)

        fun ascii(text: String) = header.write(text.toByteArray(Charsets.US_ASCII))

        fun int32(value: Int) {
            header.write(value and 0xFF)
            header.write((value shr 8) and 0xFF)
            header.write((value shr 16) and 0xFF)
            header.write((value shr 24) and 0xFF)
        }

        fun int16(value: Int) {
            header.write(value and 0xFF)
            header.write((value shr 8) and 0xFF)
        }

        ascii("RIFF")
        int32(36 + dataSize)
        ascii("WAVE")
        ascii("fmt ")
        int32(16) // fmt chunk 大小
        int16(1) // audioFormat = PCM
        int16(channels)
        int32(sampleRateHz)
        int32(byteRate)
        int16(blockAlign)
        int16(bitsPerSample)
        ascii("data")
        int32(dataSize)
        header.write(pcm)

        return header.toByteArray()
    }
}
