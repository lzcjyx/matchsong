package matchsong.data.songs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M6.1-2 SongSchemaValidator 测试：合法样例通过；非法样例逐一失败且错误指向字段。
 */
class SongSchemaValidatorTest {
    /** 单条合法歌曲 JSON（不带外层数组）。 */
    private fun validEntry(): String =
        """
        {
          "songId": "song-001", "title": "晴天", "artist": "周杰伦", "language": "zh",
          "genre": "流行", "originalKeyMidi": 62, "lowestMidi": 57, "highestMidi": 74,
          "tessituraLowMidi": 60, "tessituraHighMidi": 69, "rangeSpanSemitones": 17,
          "highNoteBurden": 0.35, "longNoteBurden": 0.28, "leapDifficulty": 0.30,
          "rhythmDifficulty": 0.40, "overallDifficulty": 0.32,
          "recommendedKeyShiftMin": -2, "recommendedKeyShiftMax": 3,
          "audioUrl": "https://example.com/a.mp3", "dataSource": "人工复核",
          "credibility": "HIGH", "dataVersion": "1.0.0", "importBatchId": "B-001"
        }
        """.trimIndent()

    private fun array(vararg entries: String): String = "[" + entries.joinToString(",") + "]"

    private fun readResource(path: String): String =
        javaClass.getResource(path)?.readText(Charsets.UTF_8)
            ?: error("测试资源缺失：$path")

    @Test
    fun `valid sample passes`() {
        assertTrue(SongSchemaValidator.validate(readResource("/sample-songs.json")).isEmpty())
        assertTrue(SongSchemaValidator.validate(array(validEntry())).isEmpty())
    }

    @Test
    fun `missing required field points to field path`() {
        val entry =
            validEntry()
                .replace("\"songId\": \"song-001\", ", "")
        val errors = SongSchemaValidator.validate(array(entry))
        assertEquals(1, errors.size)
        assertEquals("items[0].songId", errors[0].fieldPath)
        assertTrue(errors[0].reason.contains("缺少必填字段"), errors[0].reason)
    }

    @Test
    fun `lowest above highest fails with field path`() {
        val entry =
            validEntry()
                .replace("\"lowestMidi\": 57", "\"lowestMidi\": 80")
                .replace("\"highestMidi\": 74", "\"highestMidi\": 70")
        val errors = SongSchemaValidator.validate(array(entry))
        assertTrue(errors.any { it.fieldPath == "items[0].highestMidi" && it.reason.contains("低于") }, errors.toString())
    }

    @Test
    fun `burden above 1 fails with field path`() {
        val entry = validEntry().replace("\"highNoteBurden\": 0.35", "\"highNoteBurden\": 1.2")
        val errors = SongSchemaValidator.validate(array(entry))
        assertTrue(
            errors.any { it.fieldPath == "items[0].highNoteBurden" && it.reason.contains("超出") },
            errors.toString(),
        )
    }

    @Test
    fun `unknown genre fails with field path`() {
        val entry = validEntry().replace("\"genre\": \"流行\"", "\"genre\": \"布鲁斯\"")
        val errors = SongSchemaValidator.validate(array(entry))
        assertTrue(errors.any { it.fieldPath == "items[0].genre" && it.reason.contains("受控词表") }, errors.toString())
    }

    @Test
    fun `invalid language fails with field path`() {
        val entry = validEntry().replace("\"language\": \"zh\"", "\"language\": \"zh-CN\"")
        val errors = SongSchemaValidator.validate(array(entry))
        assertTrue(
            errors.any { it.fieldPath == "items[0].language" && it.reason.contains("ISO 639-1") },
            errors.toString(),
        )
    }

    @Test
    fun `midi out of range fails`() {
        val entry = validEntry().replace("\"originalKeyMidi\": 62", "\"originalKeyMidi\": 128")
        val errors = SongSchemaValidator.validate(array(entry))
        assertTrue(errors.any { it.fieldPath == "items[0].originalKeyMidi" }, errors.toString())
    }

    @Test
    fun `invalid credibility fails`() {
        val entry = validEntry().replace("\"credibility\": \"HIGH\"", "\"credibility\": \"GUESS\"")
        val errors = SongSchemaValidator.validate(array(entry))
        assertTrue(
            errors.any {
                it.fieldPath == "items[0].credibility" && it.reason.contains("HIGH/MEDIUM/LOW")
            },
            errors.toString(),
        )
    }

    @Test
    fun `original key outside range passes`() {
        // M6.3-2 语义修正：originalKey 是歌曲调性（伴奏），独立于演唱音域；
        // 超出音域合法（变调推荐正为此设计），仅校验 MIDI 范围
        val entry = validEntry().replace("\"originalKeyMidi\": 62", "\"originalKeyMidi\": 100")
        val errors = SongSchemaValidator.validate(array(entry))
        assertTrue(
            errors.none { it.fieldPath == "items[0].originalKeyMidi" },
            errors.toString(),
        )
    }

    @Test
    fun `non array top level fails`() {
        val errors = SongSchemaValidator.validate("""{"songId": "song-001"}""")
        assertEquals(1, errors.size)
        assertEquals("", errors[0].fieldPath)
        assertTrue(errors[0].reason.contains("对象数组"), errors[0].reason)
    }

    @Test
    fun `broken json fails with parse error`() {
        val errors = SongSchemaValidator.validate("{ 不是 JSON")
        assertEquals(1, errors.size)
        assertEquals("", errors[0].fieldPath)
        assertTrue(errors[0].reason.contains("解析失败"), errors[0].reason)
    }

    @Test
    fun `wrong element type fails`() {
        val errors = SongSchemaValidator.validate("""["不是对象", {}]""")
        assertTrue(errors.any { it.fieldPath == "items[0]" }, errors.toString())
    }
}
