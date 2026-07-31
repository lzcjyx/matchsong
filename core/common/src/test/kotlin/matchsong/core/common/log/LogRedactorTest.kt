package matchsong.core.common.log

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LogRedactor 测试（M1.4-3 验收：路径/设备标识/会话号被脱敏，聚合指标保留）。
 */
class LogRedactorTest {
    @Test
    fun `Android 数据目录绝对路径被脱敏`() {
        val input = "录音写入 /data/user/0/matchsong.app/cache/recordings/session.pcm 完成"
        val output = LogRedactor.redact(input)
        assertFalse(output.contains("/data/user/0/matchsong.app"))
        assertFalse(output.contains("session.pcm"))
        assertTrue(output.contains(LogRedactor.REDACTED))
    }

    @Test
    fun `storage 与 sdcard 路径被脱敏`() {
        assertFalse(
            LogRedactor.redact("读取 /storage/emulated/0/Android/data/x/cache/a.wav 失败").contains("/storage/emulated/0"),
        )
        assertFalse(LogRedactor.redact("读取 /sdcard/Download/song.wav 失败").contains("/sdcard"))
    }

    @Test
    fun `Windows 盘符路径被脱敏`() {
        val input = "写入 C:\\Users\\demo\\app\\cache\\rec.wav 失败"
        assertFalse(LogRedactor.redact(input).contains("C:\\Users\\demo"))
    }

    @Test
    fun `带前缀的 ANDROID_ID 被脱敏`() {
        val id = "a1b2c3d4e5f60718"
        val output = LogRedactor.redact("device ANDROID_ID=$id initialized")
        assertFalse(output.contains(id))
        assertTrue(output.contains(LogRedactor.REDACTED))
    }

    @Test
    fun `裸 ANDROID_ID 与 IMEI 被脱敏`() {
        val androidId = "0123456789abcdef"
        val imei = "860123456789012"
        assertFalse(LogRedactor.redact("android_id $androidId").contains(androidId))
        assertFalse(LogRedactor.redact("imei=$imei").contains(imei))
    }

    @Test
    fun `UUID 会话号被脱敏`() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val output = LogRedactor.redact("session $uuid start")
        assertFalse(output.contains(uuid))
        assertTrue(output.contains(LogRedactor.REDACTED))
    }

    @Test
    fun `聚合指标与普通文案原样保留`() {
        val input = "frameCount=1292 durationMs=30000 rmsAvg=0.42 quality=usable error=Ok"
        assertEquals(input, LogRedactor.redact(input))
    }

    @Test
    fun `空输入与纯空白原样返回`() {
        assertEquals("", LogRedactor.redact(""))
        assertEquals("   ", LogRedactor.redact("   "))
    }

    @Test
    fun `多次脱敏幂等`() {
        val input = "session 550e8400-e29b-41d4-a716-446655440000 at /data/user/0/matchsong.app/cache/a.pcm"
        val once = LogRedactor.redact(input)
        assertEquals(once, LogRedactor.redact(once))
    }
}
