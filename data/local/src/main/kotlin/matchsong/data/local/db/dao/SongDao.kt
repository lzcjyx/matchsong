package matchsong.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import matchsong.data.local.db.entity.SongMetadataEntity
import matchsong.data.local.db.entity.SongRangeProfileEntity

/**
 * 歌曲元数据 DAO（M6.4-1/3，FR-SONG-4）。
 *
 * 覆盖：批量 upsert、按 ID 查询、全量观察、搜索（标题/歌手 LIKE，M6.4-3）、
 * 组合筛选（语言/风格）、音域范围过滤（M7 候选过滤复用）、收藏歌曲关联查询。
 * 写操作全部 suspend（Room 线程安全，主线程安全由 Room 保证）。
 *
 * upsert 用 [Upsert]（INSERT … ON CONFLICT DO UPDATE）而非 REPLACE：
 * REPLACE 在 SQLite 中是「先删后插」，会级联删除外键子行（favorite/song_range_profile），
 * 破坏版本升级时保留收藏的策略（M6.4-2）。
 */
@Suppress("TooManyFunctions") // Room DAO 方法集（15 个查询/写操作）为数据访问层的合理形态，拆分反而碎片化
@Dao
interface SongDao {
    /** 批量 upsert（导入幂等基础，保留外键子行）。 */
    @Upsert
    suspend fun insertAll(songs: List<SongMetadataEntity>)

    /** 单条 upsert。 */
    @Upsert
    suspend fun insert(song: SongMetadataEntity)

    /** 批量 upsert 画像（导入时随歌曲派生落库）。 */
    @Upsert
    suspend fun insertProfiles(profiles: List<SongRangeProfileEntity>)

    @Query("SELECT * FROM song_metadata WHERE songId = :songId")
    suspend fun getById(songId: String): SongMetadataEntity?

    /** 全量观察（确定性排序：按 songId）。 */
    @Query("SELECT * FROM song_metadata ORDER BY songId")
    fun observeAll(): Flow<List<SongMetadataEntity>>

    /** 全量同步查询（确定性排序：按 songId）。 */
    @Query("SELECT * FROM song_metadata ORDER BY songId")
    suspend fun getAll(): List<SongMetadataEntity>

    /**
     * 搜索：标题/歌手包含匹配（M6.4-3）。
     *
     * SQLite LIKE 对 ASCII 大小写不敏感；中文按子串包含匹配（MVP 无分词需求）。
     */
    @Query(
        "SELECT * FROM song_metadata " +
            "WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' " +
            "ORDER BY songId",
    )
    fun search(query: String): Flow<List<SongMetadataEntity>>

    /** 组合筛选：语言/风格精确匹配（null = 不限，M6.4-3）。 */
    @Query(
        "SELECT * FROM song_metadata " +
            "WHERE (:language IS NULL OR language = :language) " +
            "AND (:genre IS NULL OR genre = :genre) " +
            "ORDER BY songId",
    )
    fun filter(
        language: String?,
        genre: String?,
    ): Flow<List<SongMetadataEntity>>

    /**
     * 音域范围过滤（M7 候选过滤）：歌曲最低音 ≤ 上界且歌曲最高音 ≥ 下界，
     * 即歌曲音域与 [lowMidi, highMidi] 有重叠。
     */
    @Query(
        "SELECT * FROM song_metadata " +
            "WHERE lowestMidi <= :highMidi AND highestMidi >= :lowMidi " +
            "ORDER BY songId",
    )
    fun getByRange(
        lowMidi: Double,
        highMidi: Double,
    ): Flow<List<SongMetadataEntity>>

    /** 收藏歌曲关联查询（按 songId 确定性排序；收藏时间倒序见 [matchsong.data.local.db.dao.FavoriteDao.observeAll]）。 */
    @Query(
        "SELECT * FROM song_metadata WHERE songId IN (SELECT songId FROM favorite) ORDER BY songId",
    )
    fun observeFavoriteSongs(): Flow<List<SongMetadataEntity>>

    /** 删除不在给定集合内的歌曲（版本升级全量替换的差量清理；级联删除其画像/收藏）。 */
    @Query("DELETE FROM song_metadata WHERE songId NOT IN (:songIds)")
    suspend fun deleteNotIn(songIds: List<String>)

    /** 清空歌曲表（级联清空画像/收藏）。 */
    @Query("DELETE FROM song_metadata")
    suspend fun clearAll()

    /** 行数。 */
    @Query("SELECT COUNT(*) FROM song_metadata")
    suspend fun count(): Int

    /** 画像行数。 */
    @Query("SELECT COUNT(*) FROM song_range_profile")
    suspend fun countProfiles(): Int

    /** 当前存储的歌曲数据版本集合（导入幂等/升级判定，M6.4-2）。 */
    @Query("SELECT DISTINCT dataVersion FROM song_metadata")
    suspend fun getDataVersions(): List<String>
}
