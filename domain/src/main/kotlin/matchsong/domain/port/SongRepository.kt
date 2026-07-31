package matchsong.domain.port

import matchsong.core.model.song.SongMetadata

/**
 * 歌曲数据仓库 Port（ARCHITECTURE.md §4.1/§7，FR-SONG-*）。
 * 由 :data:songs + :data:local 实现（M6）；Fake 实现见 core:testing（FR-SHELL-3）。
 *
 * [SongInfo] 为最小视图（UI 列表用）；[getAllMetadata] 返回完整字段（推荐引擎消费，M7）。
 */
interface SongRepository {
    /** 返回全部歌曲（确定性排序：按 songId）。 */
    suspend fun getAll(): List<SongInfo>

    suspend fun getById(songId: String): SongInfo?

    /** 返回全部歌曲完整元数据（推荐引擎输入，M7）。 */
    suspend fun getAllMetadata(): List<SongMetadata>
}

/** 歌曲元数据最小占位模型（M2/M6 细化）。 */
data class SongInfo(
    val songId: String,
    val title: String,
    val artist: String,
    val language: String,
)
