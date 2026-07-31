package matchsong.domain.port

/**
 * M3.5-2 录音临时文件清理 Port（ARCHITECTURE.md §7.3/§11，FR-REC-8）。
 *
 * 契约：删除录音缓存目录中「修改时间早于 now - [olderThanMs]」且「不属于
 * [activeSessionIds]」的 `{sessionId}.pcm` / `{sessionId}.wav` 残留文件；
 * 进行中会话（RecordingStateMachine.isActive()）的文件必须保留。
 *
 * 实现：core:audio android 子包的 [matchsong.core.audio.android.RecordingFileManager]，
 * 由 app DI 装配（app/di/AppModule.kt）。
 */
interface RecordingFileCleaner {
    /**
     * 清理过期残留录音文件（尽力而为：单文件删除失败跳过，不中断整体清理）。
     *
     * @param olderThanMs 过期阈值（毫秒）：文件修改时间早于 `now - olderThanMs` 视为过期。
     * @param activeSessionIds 进行中会话 ID 集合（其 .pcm/.wav 保留，FR-REC-8）。
     * @return 实际删除的文件数。
     */
    suspend fun cleanStale(
        olderThanMs: Long,
        activeSessionIds: Set<String>,
    ): Int
}
