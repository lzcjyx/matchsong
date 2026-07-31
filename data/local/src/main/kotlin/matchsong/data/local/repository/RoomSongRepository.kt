package matchsong.data.local.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import matchsong.core.common.time.Clock
import matchsong.data.local.db.dao.FavoriteDao
import matchsong.data.local.db.dao.SongDao
import matchsong.data.local.db.entity.FavoriteEntity
import matchsong.data.local.db.entity.SongMetadataEntity
import matchsong.domain.port.SongInfo
import matchsong.domain.port.SongRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * domain SongRepository Port 的 Room 实现（M6.4-2，ARCHITECTURE.md §3.3）。
 *
 * Port 方法（getAll/getById）返回 [SongInfo]（当前 Port 占位模型）；
 * 搜索/筛选/收藏扩展方法供 M7 候选过滤与 M8 收藏复用（M6.4-3）。
 *
 * 注：Port 的 [SongInfo] 为最小字段集（songId/title/artist/language），
 * core:model SongMetadata 落地后 Port 扩展字段时，此处映射随之丰富
 * （当前映射丢弃其余元数据字段，属 Port 合约限制，非实现缺口）。
 */
@Singleton
class RoomSongRepository
    @Inject
    constructor(
        private val songDao: SongDao,
        private val favoriteDao: FavoriteDao,
        private val clock: Clock,
    ) : SongRepository {
        override suspend fun getAll(): List<SongInfo> = songDao.getAll().map { it.toSongInfo() }

        override suspend fun getById(songId: String): SongInfo? = songDao.getById(songId)?.toSongInfo()

        // ---- M6.4-3 搜索/筛选/音域（M7 候选过滤复用） ----

        /** 全量观察（按 songId 排序）。 */
        fun observeAll(): Flow<List<SongInfo>> = songDao.observeAll().map { list -> list.map { it.toSongInfo() } }

        /** 搜索：标题/歌手包含匹配（大小写不敏感，中文子串匹配）。 */
        fun search(query: String): Flow<List<SongInfo>> =
            songDao.search(query).map { list -> list.map { it.toSongInfo() } }

        /** 组合筛选：语言/风格（null = 不限）。 */
        fun filter(
            language: String?,
            genre: String?,
        ): Flow<List<SongInfo>> = songDao.filter(language, genre).map { list -> list.map { it.toSongInfo() } }

        /** 音域范围过滤：歌曲音域与 [lowMidi, highMidi] 重叠（M7.1 候选过滤）。 */
        fun getByRange(
            lowMidi: Double,
            highMidi: Double,
        ): Flow<List<SongInfo>> = songDao.getByRange(lowMidi, highMidi).map { list -> list.map { it.toSongInfo() } }

        // ---- 收藏关系（FR-HX-2 数据侧） ----

        /** 收藏歌曲（按 songId 排序）。 */
        fun observeFavoriteSongs(): Flow<List<SongInfo>> =
            songDao.observeFavoriteSongs().map { list -> list.map { it.toSongInfo() } }

        /** 收藏歌曲 ID 列表（按收藏时间倒序）。 */
        fun observeFavoriteSongIds(): Flow<List<String>> = favoriteDao.observeFavoriteSongIds()

        suspend fun isFavorite(songId: String): Boolean = favoriteDao.isFavorite(songId)

        /** 收藏（重复收藏幂等，刷新收藏时间）。 */
        suspend fun addFavorite(songId: String) {
            favoriteDao.insert(FavoriteEntity(songId = songId, favoritedAtMs = clock.nowMillis()))
        }

        suspend fun removeFavorite(songId: String) {
            favoriteDao.delete(songId)
        }
    }

/** [SongMetadataEntity] → Port [SongInfo]（当前 Port 最小字段集映射）。 */
internal fun SongMetadataEntity.toSongInfo(): SongInfo =
    SongInfo(
        songId = songId,
        title = title,
        artist = artist,
        language = language,
    )
