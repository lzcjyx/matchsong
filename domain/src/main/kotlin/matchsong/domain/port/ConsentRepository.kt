package matchsong.domain.port

/**
 * 隐私同意记录仓库 Port（ARCHITECTURE.md §7.1 consent 表，FR-ONB-2/3，SPEC §10.6）。
 * 版本变更需重新同意；删除全部数据时清除。M6 由 Room ConsentDao 实现；
 * Fake 实现见 core:testing（FR-SHELL-3）。
 */
interface ConsentRepository {
    /** 当前已同意的隐私说明版本；从未同意返回 null。 */
    suspend fun getAcceptedVersion(): String?

    suspend fun isAccepted(version: String): Boolean

    /** 记录对指定版本隐私说明的同意。 */
    suspend fun accept(version: String)

    /** 撤销同意（删除全部数据时调用，FR-HX-4）。 */
    suspend fun revoke()
}
