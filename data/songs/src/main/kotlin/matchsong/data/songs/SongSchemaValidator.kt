package matchsong.data.songs

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import matchsong.core.model.song.Credibility
import matchsong.core.model.song.Genre

/**
 * Schema 错误（M6.1-2）：字段路径 + 原因。
 *
 * @param fieldPath 出错字段的 JSON 路径（如 `items[2].genre`）；整体结构错误时为顶层路径 `""`
 * @param reason 中文错误原因
 */
data class SchemaError(
    val fieldPath: String,
    val reason: String,
)

/**
 * 歌曲数据 JSON Schema 校验器（M6.1-2）。
 *
 * 以 [kotlinx.serialization] 解析为 [JsonElement] 后按规则校验（data-model §2.8 为单一来源），
 * 不依赖外部 JSON Schema 库；song-schema.json 为同规则的可读声明（对照维护）。
 *
 * 输入为歌曲对象数组，校验项：
 * - required 字段齐全（[REQUIRED_FIELDS]）且类型正确；
 * - 整数音高 MIDI 0~127、负担/难度 [0,1]、语言 ISO 639-1、风格受控词表（[Genre.ALL]）、
 *   可信度枚举、版本语义化格式；
 * - 交叉规则：lowest ≤ highest、tessituraLow ≤ tessituraHigh、原调在最低~最高之间。
 *
 * 注意：required 为最小契约（M6.1-2 定义）；SongMetadata 的其余非空字段
 * （跨度/负担/难度/变调范围）由 SongDataParser 在解析期保证，缺省按条目级解析错误上报。
 */
object SongSchemaValidator {
    /** 必填字段（M6.1-2）。 */
    val REQUIRED_FIELDS: List<String> =
        listOf(
            "songId", "title", "artist", "language", "genre",
            "originalKeyMidi", "lowestMidi", "highestMidi",
            "tessituraLowMidi", "tessituraHighMidi",
            "dataSource", "credibility", "dataVersion",
        )

    /** MIDI Note 合法范围（data-model §2.8：0~127）。 */
    private val MIDI_RANGE: IntRange = 0..127

    /** ISO 639-1：小写两字母。 */
    private val LANGUAGE_PATTERN: Regex = Regex("^[a-z]{2}$")

    /** 语义化版本（与 SongImportValidator 一致）。 */
    private val SEMVER_PATTERN: Regex = Regex("""^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.\-]+)?$""")

    private val json: Json = Json { ignoreUnknownKeys = true }

    /** 校验 [jsonText]（UTF-8 JSON 数组文本），返回字段级错误列表；空列表 = 通过。 */
    fun validate(jsonText: String): List<SchemaError> {
        val text = jsonText.removePrefix("\uFEFF")
        val root =
            try {
                json.parseToJsonElement(text)
            } catch (e: SerializationException) {
                return listOf(SchemaError("", "JSON 解析失败：${e.message}"))
            }
        return validate(root)
    }

    /** 校验已解析的 [root]（须为歌曲对象数组）。 */
    fun validate(root: JsonElement): List<SchemaError> {
        val errors = mutableListOf<SchemaError>()
        val array =
            root as? JsonArray
                ?: return listOf(SchemaError("", "顶层必须是歌曲对象数组"))
        array.forEachIndexed { index, element ->
            val path = "items[$index]"
            val obj = element as? JsonObject
            if (obj == null) {
                errors += SchemaError(path, "条目必须是对象，实际为 ${element::class.simpleName}")
                return@forEachIndexed
            }
            checkRequired(obj, path, errors)
            checkString(obj, "songId", path, errors) { content -> if (content.isBlank()) "songId 不能为空" else null }
            checkString(obj, "title", path, errors) { content ->
                when {
                    content.isBlank() -> "title 不能为空"
                    content.length > 200 -> "title 长度超过 200"
                    else -> null
                }
            }
            checkString(obj, "artist", path, errors) { content ->
                when {
                    content.isBlank() -> "artist 不能为空"
                    content.length > 200 -> "artist 长度超过 200"
                    else -> null
                }
            }
            checkString(obj, "language", path, errors) { content ->
                if (LANGUAGE_PATTERN.matches(content)) null else "language 必须是 ISO 639-1 两字母小写码，实际: $content"
            }
            checkString(obj, "genre", path, errors) { content ->
                if (Genre.isValid(content)) null else "genre 不在受控词表内: $content"
            }
            checkString(obj, "dataSource", path, errors) { content ->
                if (content.isBlank()) "dataSource 不能为空（FR-SONG-2）" else null
            }
            checkString(obj, "dataVersion", path, errors) { content ->
                if (SEMVER_PATTERN.matches(content)) null else "dataVersion 不是语义化版本格式，实际: $content"
            }
            checkString(obj, "credibility", path, errors) { content ->
                if (Credibility.entries.any { it.name == content }) {
                    null
                } else {
                    "credibility 必须是 HIGH/MEDIUM/LOW，实际: $content"
                }
            }
            checkInt(obj, "originalKeyMidi", path, errors, MIDI_RANGE, "原调（MIDI）")
            checkInt(obj, "lowestMidi", path, errors, MIDI_RANGE, "最低音（MIDI）")
            checkInt(obj, "highestMidi", path, errors, MIDI_RANGE, "最高音（MIDI）")
            checkInt(obj, "tessituraLowMidi", path, errors, MIDI_RANGE, "主要音区低端（MIDI）")
            checkInt(obj, "tessituraHighMidi", path, errors, MIDI_RANGE, "主要音区高端（MIDI）")
            checkInt(obj, "rangeSpanSemitones", path, errors, 0..Int.MAX_VALUE, "音域跨度")
            checkInt(obj, "recommendedKeyShiftMin", path, errors, -12..0, "推荐变调下限")
            checkInt(obj, "recommendedKeyShiftMax", path, errors, 0..12, "推荐变调上限")
            checkRatio(obj, "highNoteBurden", path, errors, "高音持续负担")
            checkRatio(obj, "longNoteBurden", path, errors, "长音负担")
            checkRatio(obj, "leapDifficulty", path, errors, "跳进难度")
            checkRatio(obj, "rhythmDifficulty", path, errors, "节奏难度")
            checkRatio(obj, "overallDifficulty", path, errors, "总体难度")
            checkCross(obj, path, errors)
        }
        return errors
    }

    private fun checkRequired(
        obj: JsonObject,
        path: String,
        errors: MutableList<SchemaError>,
    ) {
        for (field in REQUIRED_FIELDS) {
            val value = obj[field]
            if (value == null || value is JsonNull) {
                errors += SchemaError("$path.$field", "缺少必填字段")
            }
        }
    }

    /**
     * 字符串字段检查：缺省（可选字段）跳过；[valid] 返回 null 表示通过，否则为错误原因。
     */
    private fun checkString(
        obj: JsonObject,
        field: String,
        path: String,
        errors: MutableList<SchemaError>,
        valid: (String) -> String?,
    ) {
        val value = obj[field] ?: return
        if (value is JsonNull) return
        if (value !is JsonPrimitive || !value.isString) {
            errors += SchemaError("$path.$field", "必须是字符串")
            return
        }
        val reason = valid(value.content)
        if (reason != null) errors += SchemaError("$path.$field", reason)
    }

    /**
     * 整数字段检查：缺省（可选字段）跳过；[range] 外报错。
     */
    private fun checkInt(
        obj: JsonObject,
        field: String,
        path: String,
        errors: MutableList<SchemaError>,
        range: IntRange,
        label: String,
    ) {
        val value = obj[field] ?: return
        if (value is JsonNull) {
            errors += SchemaError("$path.$field", "必须是整数，不能为 null")
            return
        }
        if (value !is JsonPrimitive) {
            errors += SchemaError("$path.$field", "必须是整数")
            return
        }
        val int = value.intOrNull
        if (int == null) {
            errors += SchemaError("$path.$field", "必须是整数，实际: ${value.content}")
        } else if (int !in range) {
            errors += SchemaError("$path.$field", "$label 超出 $range，实际: $int")
        }
    }

    /**
     * 0~1 比例字段检查：缺省（可选字段）跳过。
     */
    private fun checkRatio(
        obj: JsonObject,
        field: String,
        path: String,
        errors: MutableList<SchemaError>,
        label: String,
    ) {
        val value = obj[field] ?: return
        if (value is JsonNull) {
            errors += SchemaError("$path.$field", "必须是 0..1 数值，不能为 null")
            return
        }
        if (value !is JsonPrimitive) {
            errors += SchemaError("$path.$field", "必须是 0..1 数值")
            return
        }
        val double = value.doubleOrNull
        if (double == null) {
            errors += SchemaError("$path.$field", "必须是数值，实际: ${value.content}")
        } else if (double !in 0.0..1.0) {
            errors += SchemaError("$path.$field", "$label 超出 [0,1]，实际: $double")
        }
    }

    /** 跨字段规则（仅当相关字段可解析为整数时生效）。 */
    private fun checkCross(
        obj: JsonObject,
        path: String,
        errors: MutableList<SchemaError>,
    ) {
        val lowest = obj["lowestMidi"]?.jsonPrimitive?.intOrNull
        val highest = obj["highestMidi"]?.jsonPrimitive?.intOrNull
        if (lowest != null && highest != null && lowest > highest) {
            errors += SchemaError("$path.highestMidi", "highestMidi($highest) 低于 lowestMidi($lowest)")
        }
        val tessLow = obj["tessituraLowMidi"]?.jsonPrimitive?.intOrNull
        val tessHigh = obj["tessituraHighMidi"]?.jsonPrimitive?.intOrNull
        if (tessLow != null && tessHigh != null && tessLow > tessHigh) {
            errors +=
                SchemaError(
                    "$path.tessituraHighMidi",
                    "tessituraHighMidi($tessHigh) 低于 tessituraLowMidi($tessLow)",
                )
        }
        val key = obj["originalKeyMidi"]?.jsonPrimitive?.intOrNull
        // 原调是歌曲调性（伴奏），与演唱音域独立（M6.3-2 语义修正；变调推荐正为此设计）
        if (key != null && (key < 0 || key > 127)) {
            errors += SchemaError("$path.originalKeyMidi", "originalKeyMidi($key) 超出 MIDI 范围 0..127")
        }
    }
}
