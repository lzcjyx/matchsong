package matchsong.core.testing.fake

import matchsong.domain.port.RecordingFileCleaner

/**
 * 内存实现录音文件清理 Port（M9.3，FR-PRIV-5 删除流程测试用；FR-SHELL-3）。
 *
 * 文件以会话 ID 集合建模（不含时间戳维度——过期清理语义由
 * [matchsong.domain.recording.CleanupStaleRecordingsUseCaseTest] 的专用 Fake 覆盖）；
 * 本 Fake 聚焦 [clearAll]（删除全部数据）与 [cleanStale] 的调用参数透传。
 */
class FakeRecordingFileCleaner(
    initialSessionIds: Set<String> = emptySet(),
) : RecordingFileCleaner {
    /** 当前存在的会话录音文件（会话 ID 集合）。 */
    val sessionIds: MutableSet<String> = initialSessionIds.toMutableSet()

    var lastOlderThanMs: Long? = null
    var lastActiveSessionIds: Set<String>? = null

    override suspend fun cleanStale(
        olderThanMs: Long,
        activeSessionIds: Set<String>,
    ): Int {
        lastOlderThanMs = olderThanMs
        lastActiveSessionIds = activeSessionIds
        val stale = sessionIds.filterNot { it in activeSessionIds }
        stale.forEach { sessionIds.remove(it) }
        return stale.size
    }

    override suspend fun clearAll(): Int {
        val count = sessionIds.size
        sessionIds.clear()
        return count
    }
}
