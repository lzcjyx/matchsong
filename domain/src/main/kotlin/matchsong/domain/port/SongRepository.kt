package matchsong.domain.port

/**
 * 歌曲数据仓库 Port（ARCHITECTURE.md §4.1/§7，FR-SONG-*）。
 * 由 :data:songs + :data:local 实现（M6）；Fake 实现见 core:testing（FR-SHELL-3）。
 *
 * [SongInfo] 为占位最小模型：M0.3 数据模型 SongMetadata（docs/architecture/data-model.md §2.8）
 * 落地后以 core:model 类型替换并扩展字段（M2/M6 细化）。
 */
interface SongRepository {
    /** 返回全部歌曲（确定性排序：按 songId）。 */
    suspend fun getAll(): List<SongInfo>

    suspend fun getById(songId: String): SongInfo?
}

/** 歌曲元数据最小占位模型（M2/M6 细化）。 */
data class SongInfo(
    val songId: String,
    val title: String,
    val artist: String,
    val language: String,
)
