package matchsong.core.audio.android

import matchsong.core.common.error.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * RecordingErrorMapper 映射测试（M3.3-3 验收：AudioRecord 异常 → 类型化 AppError）。
 */
class RecordingErrorMapperTest {
    @Test
    fun `SecurityException 映射为 PermissionRevoked 并保留根因`() {
        val cause = SecurityException("RECORD_AUDIO 权限被撤销")
        val error = RecordingErrorMapper.map(cause, AudioCaptureStage.READ)

        val mapped = assertInstanceOf(AppError.RecordingError.PermissionRevoked::class.java, error)
        assertSame(cause, mapped.cause)
        assertEquals(AppError.RecordingError.Reason.PermissionRevoked, mapped.reason)
        assertEquals("error.recording.permission_revoked", mapped.messageKey)
    }

    @Test
    fun `IllegalStateException 在 INIT 阶段映射为 InitFailed`() {
        val error = RecordingErrorMapper.map(IllegalStateException("state 未初始化"), AudioCaptureStage.INIT)

        val mapped = assertInstanceOf(AppError.RecordingError.InitFailed::class.java, error)
        assertEquals(AppError.RecordingError.Reason.InitFailed, mapped.reason)
        assertEquals("error.recording.init_failed", mapped.messageKey)
    }

    @Test
    fun `IllegalStateException 在 START 阶段映射为 MicBusy`() {
        val error = RecordingErrorMapper.map(IllegalStateException("startRecording 失败"), AudioCaptureStage.START)

        val mapped = assertInstanceOf(AppError.RecordingError.MicBusy::class.java, error)
        assertEquals(AppError.RecordingError.Reason.MicBusy, mapped.reason)
        assertEquals("error.recording.mic_busy", mapped.messageKey)
    }

    @Test
    fun `IllegalStateException 在 READ 阶段映射为 ReadError`() {
        val error = RecordingErrorMapper.map(IllegalStateException("read 失败"), AudioCaptureStage.READ)

        val mapped = assertInstanceOf(AppError.RecordingError.ReadError::class.java, error)
        assertEquals(AppError.RecordingError.Reason.ReadError, mapped.reason)
        assertEquals("error.recording.read_error", mapped.messageKey)
    }

    @Test
    fun `IllegalArgumentException 在 INIT 阶段映射为 InitFailed`() {
        val error = RecordingErrorMapper.map(IllegalArgumentException("采样率不受支持"), AudioCaptureStage.INIT)

        val mapped = assertInstanceOf(AppError.RecordingError.InitFailed::class.java, error)
        assertEquals(AppError.RecordingError.Reason.InitFailed, mapped.reason)
    }

    @Test
    fun `IllegalArgumentException 在 READ 阶段映射为 ReadError`() {
        val error = RecordingErrorMapper.map(IllegalArgumentException("read 参数非法"), AudioCaptureStage.READ)

        assertInstanceOf(AppError.RecordingError.ReadError::class.java, error)
    }

    @Test
    fun `未预期异常映射为 UnknownError 兜底并保留根因`() {
        val cause = RuntimeException("未预期异常")
        val error = RecordingErrorMapper.map(cause, AudioCaptureStage.INIT)

        val mapped = assertInstanceOf(AppError.UnknownError::class.java, error)
        assertSame(cause, mapped.cause)
        assertEquals("error.unknown", mapped.messageKey)
    }

    @Test
    fun `initFailed 工厂携带根因与降级细节`() {
        val cause = IllegalStateException("所有候选采样率均不可用")
        val error = RecordingErrorMapper.initFailed(cause, mapOf("attemptedRates" to "44100,48000,16000"))

        assertSame(cause, error.cause)
        assertEquals("44100,48000,16000", error.details["attemptedRates"])
        assertEquals(AppError.RecordingError.Reason.InitFailed, error.reason)
        assertEquals("error.recording.init_failed", error.messageKey)
    }
}
