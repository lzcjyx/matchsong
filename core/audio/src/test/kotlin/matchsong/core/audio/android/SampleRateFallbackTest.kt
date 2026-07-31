package matchsong.core.audio.android

import matchsong.core.common.log.Logger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * SampleRateFallback 降级链测试（M3.3-3 验收：注入 Fake 工厂，纯 JVM 无真实设备）。
 */
class SampleRateFallbackTest {
    private val logger = FakeLogger()

    @Test
    fun `44100 与 48000 不可用、16000 可用时降级到 16000`() {
        val factory = AudioRecordFactory { rate -> if (rate == 16_000) workingHandle() else null }
        val fallback = SampleRateFallback(factory, logger)

        assertEquals(16_000, fallback.findWorkingSampleRate(preferredRate = 44_100))
    }

    @Test
    fun `配置采样率可用时优先于降级链`() {
        val factory = AudioRecordFactory { rate -> if (rate == 48_000) workingHandle() else null }
        val fallback = SampleRateFallback(factory, logger)

        assertEquals(48_000, fallback.findWorkingSampleRate(preferredRate = 48_000))
    }

    @Test
    fun `全部采样率不可用返回 null`() {
        val factory = AudioRecordFactory { null }
        val fallback = SampleRateFallback(factory, logger)

        assertNull(fallback.findWorkingSampleRate(preferredRate = 44_100))
    }

    @Test
    fun `配置采样率不在降级链时仍优先尝试配置值`() {
        val factory = AudioRecordFactory { rate -> if (rate == 22_050) workingHandle() else null }
        val fallback = SampleRateFallback(factory, logger)

        assertEquals(22_050, fallback.findWorkingSampleRate(preferredRate = 22_050))
    }

    @Test
    fun `工厂抛异常视为该采样率不可用并继续降级`() {
        val factory =
            AudioRecordFactory { rate ->
                if (rate == 44_100) error("native 探测异常")
                if (rate == 16_000) workingHandle() else null
            }
        val fallback = SampleRateFallback(factory, logger)

        assertEquals(16_000, fallback.findWorkingSampleRate(preferredRate = 44_100))
        assertEquals(1, logger.warnings.size) // P9：异常被记录
    }

    @Test
    fun `降级链按 44100-48000-16000 顺序探测且命中即停`() {
        val probed = mutableListOf<Int>()
        val factory =
            AudioRecordFactory { rate ->
                probed += rate
                if (rate == 48_000) workingHandle() else null
            }
        val fallback = SampleRateFallback(factory, logger)

        assertEquals(48_000, fallback.findWorkingSampleRate(preferredRate = 44_100))
        assertEquals(listOf(44_100, 48_000), probed)
    }

    @Test
    fun `配置值在降级链中时不重复探测`() {
        val probed = mutableListOf<Int>()
        val factory =
            AudioRecordFactory { rate ->
                probed += rate
                if (rate == 44_100) workingHandle() else null
            }
        val fallback = SampleRateFallback(factory, logger)

        assertEquals(44_100, fallback.findWorkingSampleRate(preferredRate = 44_100))
        assertEquals(listOf(44_100), probed)
    }

    private fun workingHandle(): InitializedAudioRecord = object : InitializedAudioRecord {}

    private class FakeLogger : Logger {
        val warnings = mutableListOf<String>()

        override fun d(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit

        override fun i(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit

        override fun w(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) {
            warnings += message
        }

        override fun e(
            tag: String,
            message: String,
            throwable: Throwable?,
        ) = Unit
    }
}
