package matchsong.data.local.repository

import androidx.room.withTransaction
import matchsong.core.common.time.Clock
import matchsong.data.local.db.MatchSongDatabase
import matchsong.data.local.db.dao.SongDao
import matchsong.data.songs.SongDataParser
import matchsong.data.songs.SongImportValidator
import javax.inject.Inject
import javax.inject.Singleton

/** 一次歌曲导入的结果。 */
data class SongImportOutcome(
    /** 本次导入涉及的数据版本集合（校验保证批次内版本一致，实际为单值集合）。 */
    val dataVersions: Set<String>,
    /** 落库歌曲数。 */
    val importedCount: Int,
    /** true = 发生数据替换（版本差异）；false = 同版本幂等跳过。 */
    val replaced: Boolean,
)

/**
 * 歌曲初始数据导入仓库（M6.4-2，FR-SONG-4 导入/版本部分）。
 *
 * 职责：
 * 1. 解析 + 校验：委托 data:songs 的 [SongDataParser]（裸数组契约，条目级错误）
 *    与 [SongImportValidator]（字段/重复/音高派生/版本格式与批次一致性，M6.2）；
 * 2. 幂等导入：存储的 dataVersion 集合与目录一致时跳过（不写库）；
 * 3. 版本差异导入：事务内全量 upsert + 差量清理（删除不在新目录中的歌曲，
 *    级联清理其画像/收藏），失败自动回滚到旧数据（事务保证，data-model §5.2）。
 *
 * 收藏保留策略：版本升级时保留仍存在于新目录中的歌曲的收藏记录
 * （upsert 不触发外键级联）；被移除歌曲的收藏随歌曲级联删除（M6.4-1 级联策略）。
 * `importBatchId` 缺省的歌曲统一合成当前导入批次号（data-model §2.8 非空约束）。
 */
@Singleton
class SongImportRepository
    @Inject
    constructor(
        private val database: MatchSongDatabase,
        private val songDao: SongDao,
        private val clock: Clock,
    ) {
        /**
         * 导入数据集。
         *
         * @return 成功：[SongImportOutcome]；失败（JSON 非法/条目解析失败/校验失败/
         *         空数据集）：[Result.failure] 且不触碰数据库（解析与校验发生在事务外）。
         */
        suspend fun import(assetsJson: String): Result<SongImportOutcome> =
            try {
                val parsed = SongDataParser.parse(assetsJson)
                require(parsed.errors.isEmpty()) {
                    "数据集解析失败：${parsed.errors.take(3).joinToString("；") { it.reason }}"
                }
                require(parsed.songs.isNotEmpty()) { "数据集为空：无歌曲条目" }
                val validation = SongImportValidator.validate(parsed.songs)
                require(validation.isValid) {
                    "数据集校验失败：${validation.entryErrors.take(3).joinToString("；") { it.reason }}"
                }
                val songs = parsed.songs
                val importedVersions = songs.map { it.dataVersion }.toSet()
                val storedVersions = songDao.getDataVersions().toSet()
                if (storedVersions == importedVersions) {
                    // 幂等：版本集合一致不重复导入（M6.4-2）
                    Result.success(
                        SongImportOutcome(
                            dataVersions = importedVersions,
                            importedCount = songDao.count(),
                            replaced = false,
                        ),
                    )
                } else {
                    val entities = songs.toEntities(fallbackBatchId = "import-${clock.nowMillis()}")
                    val profiles = songs.toProfiles()
                    // 事务性导入：任一步失败整体回滚，旧数据保持可用（升级中断安全）
                    database.withTransaction {
                        songDao.insertAll(entities)
                        songDao.insertProfiles(profiles)
                        songDao.deleteNotIn(entities.map { it.songId })
                    }
                    Result.success(
                        SongImportOutcome(
                            dataVersions = importedVersions,
                            importedCount = entities.size,
                            replaced = true,
                        ),
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
    }
