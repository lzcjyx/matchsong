package matchsong.data.local.network

import matchsong.data.local.repository.SongImportOutcome
import matchsong.data.local.repository.SongImportRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BUG-018 歌曲包导入器：下载远程歌曲包 JSON → 复用 [SongImportRepository] 幂等导入。
 *
 * 导入语义（与内置曲库同源）：dataVersion 集合一致跳过；版本差异事务替换
 * （upsert + 差量清理 + 收藏保留）；失败不触碰数据库。
 *
 * 合规：仅下载公开歌曲元数据；不上传任何数据（FR-PRIV-3）。
 */
@Singleton
class SongPackImporter
    @Inject
    constructor(
        private val fetcher: SongPackFetcher,
        private val importRepository: SongImportRepository,
    ) {
        /**
         * 下载并导入歌曲包。
         *
         * @return 成功携带 [SongImportOutcome]；失败（网络/非 2xx/超限/解析校验失败）
         *         返回 [Result.failure]，数据库保持原状。
         */
        suspend fun importPack(packUrl: String): Result<SongImportOutcome> =
            try {
                val json = fetcher.fetch(packUrl)
                importRepository.import(json)
            } catch (e: Exception) {
                Result.failure(e)
            }
    }
