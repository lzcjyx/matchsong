package matchsong.data.songs

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import matchsong.core.model.song.SongMetadata

/**
 * 条目级错误（M6.2）。
 *
 * @param index 条目序号：JSON = 数组下标（0 起）；CSV = 文件行号（1 起，表头为第 1 行，
 *   首条数据为第 2 行）；导入校验 = 歌曲列表下标（0 起）
 * @param reason 中文错误原因（含字段名）
 */
data class EntryError(
    val index: Int,
    val reason: String,
)

/**
 * 解析结果（M6.2-1）。
 *
 * @param songs 成功解析的歌曲（按文件出现顺序）
 * @param errors 解析失败条目（每条对应一个 [EntryError]，含位置）
 */
data class ParserResult(
    val songs: List<SongMetadata>,
    val errors: List<EntryError>,
) {
    /** 文件中条目总数（= 成功 + 失败）。 */
    val totalEntryCount: Int get() = songs.size + errors.size

    /** 全部条目是否解析成功。 */
    val isClean: Boolean get() = errors.isEmpty()
}

/**
 * 歌曲数据 JSON 解析器（M6.2-1）。
 *
 * 输入为歌曲对象数组（与 song-schema.json 契约对齐，字段名见 SongMetadata）；
 * 单条目解析失败（缺字段/类型错误/null 注入非空字段）上报为带数组下标的 [EntryError]，
 * 不影响其余条目。支持 UTF-8 BOM；未知字段忽略（前向兼容）。
 */
object SongDataParser {
    private val json: Json = Json { ignoreUnknownKeys = true }

    /** 解析 [jsonText] 为歌曲列表 + 条目级错误。 */
    fun parse(jsonText: String): ParserResult {
        val text = jsonText.removePrefix("\uFEFF")
        val root =
            try {
                json.parseToJsonElement(text)
            } catch (e: SerializationException) {
                return ParserResult(emptyList(), listOf(EntryError(0, "JSON 解析失败：${e.message}")))
            }
        val array =
            root as? JsonArray
                ?: return ParserResult(emptyList(), listOf(EntryError(0, "顶层必须是歌曲对象数组")))
        val songs = mutableListOf<SongMetadata>()
        val errors = mutableListOf<EntryError>()
        array.forEachIndexed { index, element ->
            try {
                songs += json.decodeFromJsonElement<SongMetadata>(element)
            } catch (e: SerializationException) {
                errors += EntryError(index, "条目解析失败：${e.message}")
            }
        }
        return ParserResult(songs, errors)
    }
}
