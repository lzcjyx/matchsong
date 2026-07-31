package matchsong.core.common.result

import matchsong.core.common.error.AppError

/**
 * 统一操作结果类型（ARCHITECTURE.md §12.1，M1.4-1）。
 *
 * 用例返回值统一使用本类型：[Success] 携带数据，[Failure] 携带类型化错误 [AppError]。
 * 领域层可空结果一律用 `Failure(…InsufficientData)` 表达，而非裸 null。
 * 协程取消通过 [kotlinx.coroutines.CancellationException] 正常传播（结构化并发），不包装为业务错误。
 */
sealed interface OperationResult<out T> {
    /** 操作成功，携带结果数据。 */
    data class Success<T>(val data: T) : OperationResult<T>

    /** 操作失败，携带类型化错误。 */
    data class Failure(val error: AppError) : OperationResult<Nothing>
}
