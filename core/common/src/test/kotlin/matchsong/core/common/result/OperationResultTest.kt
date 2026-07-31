package matchsong.core.common.result

import matchsong.core.common.error.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * OperationResult 测试（M1.4-1 验收：when 分支穷尽性、Success/Failure 构造）。
 */
class OperationResultTest {
    @Test
    fun `when 分支穷尽（sealed 保证编译期检查，无 else 分支）`() {
        val result: OperationResult<String> = OperationResult.Success("ok")
        val outcome =
            when (result) {
                is OperationResult.Success -> "success: ${result.data}"
                is OperationResult.Failure -> "failure: ${result.error.messageKey}"
            }
        assertEquals("success: ok", outcome)
    }

    @Test
    fun `Success 携带数据且类型协变`() {
        val result: OperationResult<Int> = OperationResult.Success(42)
        val value =
            when (result) {
                is OperationResult.Success -> result.data
                is OperationResult.Failure -> 0
            }
        assertEquals(42, value)
    }

    @Test
    fun `Failure 携带类型化错误且可赋给任意类型参数`() {
        val error = AppError.StorageError.NoSpace
        val result: OperationResult<Int> = OperationResult.Failure(error)
        val value =
            when (result) {
                is OperationResult.Success -> result.data
                is OperationResult.Failure -> {
                    assertEquals(error, result.error)
                    -1
                }
            }
        assertEquals(-1, value)
    }
}
