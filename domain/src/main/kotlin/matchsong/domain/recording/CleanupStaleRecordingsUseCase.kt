package matchsong.domain.recording

import matchsong.domain.port.RecordingFileCleaner

/**
 * M3.5-2 启动时清理过期录音残留用例（FR-REC-8，ARCHITECTURE.md §7.3/§11，P8）。
 *
 * 编排 [RecordingFileCleaner] Port：删除过期且非活跃会话的临时 PCM/WAV，
 * 保留进行中会话的文件。活跃会话集合由调用方按
 * [RecordingStateMachine.isActive] 收集后传入（录音流程集成处），
 * 应用启动场景为全空（新进程无进行中会话）。
 *
 * 纯 Kotlin，无 Android 依赖；调度由调用方（app 启动，Dispatchers.Default）决定。
 */
class CleanupStaleRecordingsUseCase(
    private val cleaner: RecordingFileCleaner,
) {
    /**
     * 执行过期残留清理。
     *
     * @param olderThanMs 过期阈值（毫秒），默认 [DEFAULT_STALE_MS]（24h）。
     * @param activeSessionIds 进行中会话 ID 集合（默认空集 = 启动场景）。
     * @return 删除的文件数。
     */
    suspend operator fun invoke(
        olderThanMs: Long = DEFAULT_STALE_MS,
        activeSessionIds: Set<String> = emptySet(),
    ): Int = cleaner.cleanStale(olderThanMs, activeSessionIds)

    companion object {
        /**
         * 默认过期阈值：24 小时。
         * 录音单次最长 30s（FR-REC-2）+ 分析期，正常文件不会跨天存活；
         * 跨天残留视为崩溃遗留（P8），启动时删除。
         */
        const val DEFAULT_STALE_MS: Long = 86_400_000L
    }
}
