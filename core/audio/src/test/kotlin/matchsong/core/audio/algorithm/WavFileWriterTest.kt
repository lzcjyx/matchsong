package matchsong.core.audio.algorithm

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random

/**
 * M3.5-1 WAV 写入/读取测试（FR-REC-7）。
 *
 * 关键断言：产物与 core:testing WavTestFileFactory（test-fixture-manifest.md §3.1）
 * 字节级一致（黄金头比对），且 writer→reader 往返数据无损。
 */
class WavFileWriterTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `writePcmToWav 产出与 WavTestFileFactory 一致的 44 字节标准头`() {
        // PCM = 2 个 16bit 小端样本：258（0x0102）、-3（0xFFFD）
        val pcm = byteArrayOf(0x02, 0x01, 0xFD.toByte(), 0xFF.toByte())
        val pcmFile = File(tempDir, "golden.pcm").apply { writeBytes(pcm) }
        val wavFile = File(tempDir, "golden.wav")

        WavFileWriter.writePcmToWav(pcmFile, wavFile, sampleRateHz = 44_100, channels = 1, bitsPerSample = 16)

        val expected =
            byteArrayOf(
                // "RIFF"
                0x52, 0x49, 0x46, 0x46,
                // chunkSize = 36 + dataSize(4) = 40
                0x28, 0x00, 0x00, 0x00,
                // "WAVE"
                0x57, 0x41, 0x56, 0x45,
                // "fmt "
                0x66, 0x6D, 0x74, 0x20,
                // fmtSize = 16
                0x10, 0x00, 0x00, 0x00,
                // audioFormat = 1 (PCM)
                0x01, 0x00,
                // channels = 1
                0x01, 0x00,
                // sampleRate = 44100 (0xAC44)
                0x44, 0xAC.toByte(), 0x00, 0x00,
                // byteRate = 88200 (0x15888)
                0x88.toByte(), 0x58.toByte(), 0x01, 0x00,
                // blockAlign = 2
                0x02, 0x00,
                // bitsPerSample = 16
                0x10, 0x00,
                // "data"
                0x64, 0x61, 0x74, 0x61,
                // dataSize = 4
                0x04, 0x00, 0x00, 0x00,
                // PCM
                0x02, 0x01, 0xFD.toByte(), 0xFF.toByte(),
            )
        assertArrayEquals(expected, wavFile.readBytes())
    }

    @Test
    fun `写入后可经 WavFileReader 读回且样本与头部字段一致`() {
        val random = Random(42)
        val sampleCount = 2000
        val shorts = ShortArray(sampleCount) { random.nextInt().toShort() }
        val pcm =
            ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
                shorts.forEach { putShort(it) }
            }.array()
        val pcmFile = File(tempDir, "roundtrip.pcm").apply { writeBytes(pcm) }
        val wavFile = File(tempDir, "roundtrip.wav")

        WavFileWriter.writePcmToWav(pcmFile, wavFile, sampleRateHz = 44_100, channels = 1, bitsPerSample = 16)

        assertEquals(44L + pcm.size, wavFile.length())
        val data = WavFileReader().read(wavFile)
        assertEquals(44_100, data.sampleRateHz)
        assertEquals(1, data.channels)
        assertEquals(16, data.bitsPerSample)
        assertEquals(sampleCount, data.frameCount)
        assertEquals(sampleCount.toDouble() / 44_100.0, data.durationSec, 1e-9)
        assertArrayEquals(shorts, data.samples)
    }

    @Test
    fun `16bit 立体声写入读回后帧数正确且样本交织`() {
        val shorts = shortArrayOf(100, -200, 300, -400) // 2 帧 × 2 声道
        val pcm =
            ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).apply {
                shorts.forEach { putShort(it) }
            }.array()
        val pcmFile = File(tempDir, "stereo.pcm").apply { writeBytes(pcm) }
        val wavFile = File(tempDir, "stereo.wav")

        WavFileWriter.writePcmToWav(pcmFile, wavFile, sampleRateHz = 44_100, channels = 2, bitsPerSample = 16)

        val data = WavFileReader().read(wavFile)
        assertEquals(2, data.channels)
        assertEquals(2, data.frameCount)
        assertArrayEquals(shorts, data.samples)
    }

    @Test
    fun `wrapPcm 与 writePcmToWav 产出相同字节`() {
        val pcm = ByteArray(100) { it.toByte() }
        val pcmFile = File(tempDir, "cmp.pcm").apply { writeBytes(pcm) }
        val wavFile = File(tempDir, "cmp.wav")
        WavFileWriter.writePcmToWav(pcmFile, wavFile, sampleRateHz = 44_100, channels = 1, bitsPerSample = 16)

        assertArrayEquals(
            wavFile.readBytes(),
            WavFileWriter.wrapPcm(pcm, sampleRateHz = 44_100, channels = 1, bitsPerSample = 16),
        )
    }

    @Test
    fun `归一化样本与 AudioChunk 同格式且在界内`() {
        val pcmFile =
            File(tempDir, "norm.pcm").apply {
                writeBytes(byteArrayOf(0x00, 0x80.toByte(), 0xFF.toByte(), 0x7F.toByte()))
            }
        val wavFile = File(tempDir, "norm.wav")
        WavFileWriter.writePcmToWav(pcmFile, wavFile, 44_100, 1, 16)

        val normalized = WavFileReader().read(wavFile).normalizedSamples()
        assertEquals(2, normalized.size)
        // 0x8000 / 0x7FFF ≈ -1.00003、+1.0（Short.MIN/MAX 归一化非精确 ±1）
        assertEquals(-1.0f, normalized[0], 1e-4f)
        assertEquals(1.0f, normalized[1], 1e-4f)
    }

    @Test
    fun `非法格式参数抛 IllegalArgumentException`() {
        val pcmFile = File(tempDir, "bad.pcm").apply { writeBytes(byteArrayOf(0, 0)) }
        val wavFile = File(tempDir, "bad.wav")
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            WavFileWriter.writePcmToWav(pcmFile, wavFile, sampleRateHz = 0, channels = 1, bitsPerSample = 16)
        }
    }

    @Test
    fun `缺失 PCM 文件抛 IOException`() {
        val wavFile = File(tempDir, "missing.wav")
        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException::class.java) {
            WavFileWriter.writePcmToWav(File(tempDir, "nope.pcm"), wavFile, 44_100, 1, 16)
        }
    }
}
