package matchsong.data.local.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** BUG-018 歌曲包下载端口（可测试替换；实现 [HttpSongPackFetcher]）。 */
interface SongPackFetcher {
    /** 下载歌曲包 JSON 文本（仅 GET；失败抛 [IOException]）。 */
    suspend fun fetch(url: String): String
}

/**
 * BUG-018 歌曲包下载器（联网曲库扩展）。
 *
 * 职责：从 HTTPS URL 下载歌曲数据 JSON（GET）。**仅下载**——无任何上传路径
 * （FR-PRIV-3 保持：不上传音频/声音特征/个人信息）。
 *
 * 实现选择：HttpURLConnection（零新依赖）；超时 15s；大小上限 5MB
 * （MVP 数据集 50 首 ≈ 32KB，5MB 对应约 7000+ 首，防恶意超大响应）。
 * 错误语义：网络失败/非 2xx/超限 → [IOException]，由调用方映射。
 */
@Singleton
class HttpSongPackFetcher
    @Inject
    constructor() : SongPackFetcher {
        override suspend fun fetch(url: String): String =
            withContext(Dispatchers.IO) {
                val connection =
                    URL(url).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS
                    connection.setRequestProperty("Accept", "application/json")
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        throw IOException("歌曲包下载失败：HTTP $code")
                    }
                    val stream =
                        connection.inputStream ?: throw IOException("歌曲包下载失败：无响应体")
                    val bytes = stream.use { it.readBytes() }
                    if (bytes.size > MAX_PACK_BYTES) {
                        throw IOException("歌曲包过大：${bytes.size / 1024}KB（上限 ${MAX_PACK_BYTES / 1024}KB）")
                    }
                    bytes.toString(Charsets.UTF_8)
                } finally {
                    connection.disconnect()
                }
            }

        companion object {
            const val CONNECT_TIMEOUT_MS = 15_000
            const val READ_TIMEOUT_MS = 15_000
            const val MAX_PACK_BYTES = 5 * 1024 * 1024
        }
    }
