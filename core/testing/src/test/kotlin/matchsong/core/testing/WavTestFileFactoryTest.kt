package matchsong.core.testing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * WavTestFileFactory ↔ WavReader 回读测试（M1.4-4 验收：header 字段正确、信号符合解析式）。
 */
class WavTestFileFactoryTest {
    @Test
    fun `正弦 WAV 回读 header 字段正确`() {
        val wav =
            WavReader().read(
                WavTestFileFactory.create(durationSec = 1.0, signalType = WavTestFileFactory.SignalType.SINE),
            )
        assertEquals(44_100, wav.sampleRateHz)
        assertEquals(1, wav.channels)
        assertEquals(16, wav.bitsPerSample)
        assertEquals(44_100, wav.frameCount)
        assertEquals(1.0, wav.durationSec, 1e-6)
    }

    @Test
    fun `正弦样本值符合解析式`() {
        val sampleRate = 44_100
        val frequencyHz = 440.0
        val amplitude = 0.5
        val wav =
            WavReader().read(
                WavTestFileFactory.create(0.1, WavTestFileFactory.SignalType.SINE, frequencyHz, amplitude),
            )
        val index = 100
        val expected =
            (amplitude * sin(2.0 * PI * frequencyHz * index / sampleRate) * Short.MAX_VALUE)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        assertEquals(expected, wav.samples[index])
        assertTrue(wav.samples.size > index)
    }

    @Test
    fun `静音输出全零样本`() {
        val wav =
            WavReader().read(
                WavTestFileFactory.create(0.1, WavTestFileFactory.SignalType.SILENCE),
            )
        assertTrue(wav.samples.all { it == 0.toShort() })
    }

    @Test
    fun `白噪声幅值不超过配置且可复现`() {
        val amplitude = 0.5
        val factory = { WavTestFileFactory.create(0.2, WavTestFileFactory.SignalType.NOISE, amplitude = amplitude) }
        val first = WavReader().read(factory())
        val second = WavReader().read(factory())
        assertTrue(first.samples.all { abs(it.toInt()) <= (amplitude * Short.MAX_VALUE).toInt() + 1 })
        assertTrue(first.samples.contentEquals(second.samples), "固定种子输出应可复现")
    }

    @Test
    fun `削波样本被限幅在 clipLevel 且确实发生削波`() {
        val clipLevel = 0.3
        val wav =
            WavReader().read(
                WavTestFileFactory.create(
                    0.5,
                    WavTestFileFactory.SignalType.CLIPPED,
                    frequencyHz = 440.0,
                    amplitude = 0.9,
                    clipLevel = clipLevel,
                ),
            )
        val limit = (clipLevel * Short.MAX_VALUE).toInt()
        val maxAbs = wav.samples.maxOf { abs(it.toInt()) }
        assertTrue(maxAbs <= limit + 1, "削波样本不应超过限幅")
        assertTrue(maxAbs >= limit - 1, "应存在被削到限幅的样本")
    }

    @Test
    fun `写文件后可回读且内容一致`() {
        val file = File(System.getProperty("java.io.tmpdir"), "matchsong_wav_test_${System.nanoTime()}.wav")
        try {
            val bytes = WavTestFileFactory.create(0.2, WavTestFileFactory.SignalType.SINE, frequencyHz = 880.0)
            WavTestFileFactory.writeToFile(file, 0.2, WavTestFileFactory.SignalType.SINE, frequencyHz = 880.0)
            val wav = WavReader().read(file)
            assertEquals(44_100, wav.sampleRateHz)
            assertEquals(1, wav.channels)
            assertEquals(16, wav.bitsPerSample)
            assertTrue(wav.samples.isNotEmpty())
            assertTrue(wav.samples.size == (bytes.size - 44) / 2, "样本字节数 = 文件字节数 - 44 字节 header")
        } finally {
            file.delete()
        }
    }

    @Test
    fun `非法参数抛出异常`() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            WavTestFileFactory.create(durationSec = 0.0)
        }
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            WavTestFileFactory.create(durationSec = 0.1, amplitude = 1.5)
        }
    }
}
