package matchsong.core.common.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AppError 错误层级测试（M1.4-1 验收：各错误类型构造、messageKey 映射）。
 */
class AppErrorTest {
    @Test
    fun `权限错误子类携带预期 messageKey`() {
        assertEquals("error.permission.not_requested", AppError.PermissionError.NotRequested.messageKey)
        assertEquals("error.permission.denied", AppError.PermissionError.Denied.messageKey)
        assertEquals("error.permission.permanently_denied", AppError.PermissionError.PermanentlyDenied.messageKey)
        assertEquals("error.permission.unavailable", AppError.PermissionError.Unavailable.messageKey)
    }

    @Test
    fun `录音错误子类携带预期 messageKey`() {
        assertEquals("error.recording.init_failed", AppError.RecordingError.InitFailed().messageKey)
        assertEquals("error.recording.interrupted", AppError.RecordingError.Interrupted.messageKey)
        assertEquals("error.recording.canceled", AppError.RecordingError.Canceled.messageKey)
    }

    @Test
    fun `录音初始化失败携带根因且叶子错误无根因`() {
        val cause = RuntimeException("麦克风被占用")
        val error = AppError.RecordingError.InitFailed(cause = cause)
        assertEquals(cause, error.cause)
        assertNull(AppError.RecordingError.Interrupted.cause)
    }

    @Test
    fun `质量错误子类携带预期 messageKey`() {
        assertEquals("error.quality.too_short", AppError.QualityError.TooShort.messageKey)
        assertEquals("error.quality.silent", AppError.QualityError.Silent.messageKey)
        assertEquals("error.quality.too_quiet", AppError.QualityError.TooQuiet.messageKey)
        assertEquals("error.quality.noisy", AppError.QualityError.Noisy.messageKey)
        assertEquals("error.quality.clipping", AppError.QualityError.Clipping.messageKey)
        assertEquals(
            "error.quality.insufficient_valid_frames",
            AppError.QualityError.InsufficientValidFrames.messageKey,
        )
    }

    @Test
    fun `分析错误子类携带预期 messageKey`() {
        assertEquals("error.analysis.canceled", AppError.AnalysisError.Canceled.messageKey)
        assertEquals("error.analysis.insufficient_data", AppError.AnalysisError.InsufficientData.messageKey)
        assertEquals("error.analysis.internal", AppError.AnalysisError.Internal().messageKey)
    }

    @Test
    fun `存储与数据库错误携带预期 messageKey`() {
        assertEquals("error.storage.no_space", AppError.StorageError.NoSpace.messageKey)
        assertEquals("error.storage.io", AppError.StorageError.Io().messageKey)
        assertEquals("error.storage.corrupt_file", AppError.StorageError.CorruptFile.messageKey)
        assertEquals("error.database.query", AppError.DatabaseError.Query().messageKey)
        assertEquals("error.database.insert", AppError.DatabaseError.Insert().messageKey)
        assertEquals("error.database.corrupt", AppError.DatabaseError.Corrupt().messageKey)
    }

    @Test
    fun `UnknownError 兜底并保留堆栈`() {
        val cause = IllegalStateException("boom")
        val error = AppError.UnknownError(cause)
        assertEquals("error.unknown", error.messageKey)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `错误可携带结构化 details`() {
        val error = AppError.StorageError.Io(details = mapOf("path" to "recordings/s1.pcm"))
        assertEquals("recordings/s1.pcm", error.details["path"])
    }

    @Test
    fun `全部叶子错误的 messageKey 均存在于用户文案映射表`() {
        val allLeaves =
            listOf(
                AppError.PermissionError.NotRequested,
                AppError.PermissionError.Denied,
                AppError.PermissionError.PermanentlyDenied,
                AppError.PermissionError.Unavailable,
                AppError.RecordingError.InitFailed(),
                AppError.RecordingError.Interrupted,
                AppError.RecordingError.Canceled,
                AppError.QualityError.TooShort,
                AppError.QualityError.Silent,
                AppError.QualityError.TooQuiet,
                AppError.QualityError.Noisy,
                AppError.QualityError.Clipping,
                AppError.QualityError.InsufficientValidFrames,
                AppError.AnalysisError.Canceled,
                AppError.AnalysisError.InsufficientData,
                AppError.AnalysisError.Internal(),
                AppError.StorageError.NoSpace,
                AppError.StorageError.Io(),
                AppError.StorageError.CorruptFile,
                AppError.DatabaseError.Query(),
                AppError.DatabaseError.Insert(),
                AppError.DatabaseError.Corrupt(),
                AppError.UnknownError(),
            )
        allLeaves.forEach { error ->
            assertTrue(
                error.messageKey in AppError.EXAMPLE_USER_MESSAGES,
                "messageKey 缺少用户文案映射: ${error.messageKey}",
            )
        }
    }

    @Test
    fun `映射表文案与 ARCHITECTURE 12-3 示例一致`() {
        assertEquals("需要麦克风权限才能测试", AppError.EXAMPLE_USER_MESSAGES["error.permission.denied"])
        assertEquals("录音被来电中断，本次结果可能不完整", AppError.EXAMPLE_USER_MESSAGES["error.recording.interrupted"])
        assertEquals("出错了，请重试", AppError.EXAMPLE_USER_MESSAGES["error.unknown"])
    }
}
