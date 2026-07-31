package matchsong.data.songs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M6.2-1 SongDataParser / CsvSongParser 测试：合法文件 → 正确对象列表；损坏 → 条目级错误。
 */
class SongDataParserTest {
    private fun readResource(path: String): String =
        javaClass.getResource(path)?.readText(Charsets.UTF_8)
            ?: error("测试资源缺失：$path")

    // ---------- JSON ----------

    @Test
    fun `valid json parses all songs`() {
        val result = SongDataParser.parse(readResource("/sample-songs.json"))
        assertTrue(result.isClean, "合法 JSON 不应有解析错误：${result.errors}")
        assertEquals(3, result.songs.size)
        assertEquals(3, result.totalEntryCount)

        val first = result.songs[0]
        assertEquals("song-001", first.songId)
        assertEquals("晴天", first.title)
        assertEquals("周杰伦", first.artist)
        assertEquals("zh", first.language)
        assertEquals("流行", first.genre)
        assertEquals(62, first.originalKeyMidi)
        assertEquals(57, first.lowestMidi)
        assertEquals(74, first.highestMidi)
        assertEquals(0.35, first.highNoteBurden)
        assertEquals(0.32, first.overallDifficulty)
        assertEquals(-2, first.recommendedKeyShiftMin)
        assertEquals(3, first.recommendedKeyShiftMax)
        assertEquals("https://example.com/qingtian.mp3", first.audioUrl)
        assertEquals("HIGH", first.credibility.name)
        assertEquals("1.0.0", first.dataVersion)
        assertEquals("B-20260731-001", first.importBatchId)

        // song-002 缺 audioUrl / importBatchId → 默认 null
        assertNull(result.songs[1].audioUrl)
        assertNull(result.songs[1].importBatchId)
        assertEquals("R&B", result.songs[2].genre)
    }

    @Test
    fun `single broken entry reports entry level error with index`() {
        val json =
            """
            [
              {
                "songId": "song-001", "title": "晴天", "artist": "周杰伦", "language": "zh",
                "genre": "流行", "originalKeyMidi": 62, "lowestMidi": 57, "highestMidi": 74,
                "tessituraLowMidi": 60, "tessituraHighMidi": 69, "rangeSpanSemitones": 17,
                "highNoteBurden": 0.35, "longNoteBurden": 0.28, "leapDifficulty": 0.3,
                "rhythmDifficulty": 0.4, "overallDifficulty": 0.32,
                "recommendedKeyShiftMin": -2, "recommendedKeyShiftMax": 3,
                "dataSource": "人工复核", "credibility": "HIGH", "dataVersion": "1.0.0"
              },
              {
                "songId": "song-002", "title": "缺风格", "artist": "某歌手", "language": "zh",
                "originalKeyMidi": 60, "lowestMidi": 50, "highestMidi": 70,
                "tessituraLowMidi": 55, "tessituraHighMidi": 65, "rangeSpanSemitones": 20,
                "highNoteBurden": 0.2, "longNoteBurden": 0.2, "leapDifficulty": 0.2,
                "rhythmDifficulty": 0.2, "overallDifficulty": 0.2,
                "recommendedKeyShiftMin": -1, "recommendedKeyShiftMax": 1,
                "dataSource": "人工复核", "credibility": "MEDIUM", "dataVersion": "1.0.0"
              },
              {
                "songId": "song-003", "title": "小幸运", "artist": "田馥甄", "language": "zh",
                "genre": "流行", "originalKeyMidi": 64, "lowestMidi": 55, "highestMidi": 78,
                "tessituraLowMidi": 58, "tessituraHighMidi": 71, "rangeSpanSemitones": 23,
                "highNoteBurden": 0.45, "longNoteBurden": 0.5, "leapDifficulty": 0.4,
                "rhythmDifficulty": 0.55, "overallDifficulty": 0.48,
                "recommendedKeyShiftMin": -3, "recommendedKeyShiftMax": 2,
                "dataSource": "公开 MIDI 数据集", "credibility": "LOW", "dataVersion": "1.0.0"
              }
            ]
            """.trimIndent()
        val result = SongDataParser.parse(json)
        assertEquals(2, result.songs.size, "仅缺 genre 的条目应失败，其余成功")
        assertEquals(1, result.errors.size)
        assertEquals(1, result.errors[0].index, "错误应定位到数组下标 1")
        assertTrue(result.errors[0].reason.contains("genre"), result.errors[0].reason)
        assertEquals(3, result.totalEntryCount)
        assertTrue(result.songs.map { it.songId }.containsAll(listOf("song-001", "song-003")))
    }

    @Test
    fun `top level non array fails`() {
        val result = SongDataParser.parse("""{"songId": "song-001"}""")
        assertTrue(result.songs.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].reason.contains("对象数组"), result.errors[0].reason)
    }

    @Test
    fun `broken json fails`() {
        val result = SongDataParser.parse("{ 不是 JSON")
        assertTrue(result.songs.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].reason.contains("解析失败"), result.errors[0].reason)
    }

    @Test
    fun `json with bom parses`() {
        val result = SongDataParser.parse("\uFEFF" + readResource("/sample-songs.json"))
        assertEquals(3, result.songs.size)
        assertTrue(result.isClean)
    }

    // ---------- CSV ----------

    @Test
    fun `valid csv parses all songs`() {
        val result = CsvSongParser.parse(readResource("/sample-songs.csv"))
        assertTrue(result.isClean, "合法 CSV 不应有解析错误：${result.errors}")
        assertEquals(3, result.songs.size)
        assertEquals(3, result.totalEntryCount)

        val first = result.songs[0]
        assertEquals("song-001", first.songId)
        assertEquals("晴天", first.title)
        assertEquals(62, first.originalKeyMidi)
        assertEquals(0.35, first.highNoteBurden)
        assertEquals("https://example.com/qingtian.mp3", first.audioUrl)
        assertEquals("HIGH", first.credibility.name)

        // 空单元格 → null
        assertNull(result.songs[1].audioUrl)
        assertNull(result.songs[1].importBatchId)
    }

    @Test
    fun `csv with bom quoted commas and escaped quotes parses`() {
        val header =
            "songId,title,artist,language,genre,originalKeyMidi,lowestMidi,highestMidi," +
                "tessituraLowMidi,tessituraHighMidi,rangeSpanSemitones,highNoteBurden,longNoteBurden," +
                "leapDifficulty,rhythmDifficulty,overallDifficulty,recommendedKeyShiftMin," +
                "recommendedKeyShiftMax,audioUrl,dataSource,credibility,dataVersion,importBatchId"
        // 引号包裹含逗号字段、RFC 4180 双引号转义（""）、文件开头 BOM
        val csv =
            "\uFEFF$header\n" +
                "song-010,\"晴天, 现场版\",\"周杰伦\"\"Jay\"\"\",zh,流行,62,57,74,60,69,17," +
                "0.35,0.28,0.30,0.40,0.32,-2,3,https://example.com/a.mp3,人工复核,HIGH,1.0.0,B-002\n"
        val result = CsvSongParser.parse(csv)
        assertTrue(result.isClean, "应处理 BOM/引号逗号/转义引号：${result.errors}")
        assertEquals(1, result.songs.size)
        assertEquals("晴天, 现场版", result.songs[0].title)
        assertEquals("周杰伦\"Jay\"", result.songs[0].artist)
    }

    @Test
    fun `csv type error reports line number`() {
        val csv =
            """
            songId,title,artist,language,genre,originalKeyMidi,lowestMidi,highestMidi,tessituraLowMidi,tessituraHighMidi,rangeSpanSemitones,highNoteBurden,longNoteBurden,leapDifficulty,rhythmDifficulty,overallDifficulty,recommendedKeyShiftMin,recommendedKeyShiftMax,audioUrl,dataSource,credibility,dataVersion,importBatchId
            song-001,晴天,周杰伦,zh,流行,62,57,74,60,69,17,0.35,0.28,0.30,0.40,0.32,-2,3,,人工复核,HIGH,1.0.0,B-001
            song-002,坏数据,歌手,zh,流行,abc,50,70,55,65,20,0.2,0.2,0.2,0.2,0.2,-1,1,,测试,MEDIUM,1.0.0,
            """.trimIndent()
        val result = CsvSongParser.parse(csv)
        assertEquals(1, result.songs.size, "类型错误行应失败，其余行成功")
        assertEquals(1, result.errors.size)
        assertEquals(3, result.errors[0].index, "首条数据在第 2 行，坏数据在第 3 行")
        assertTrue(result.errors[0].reason.contains("originalKeyMidi"), result.errors[0].reason)
    }

    @Test
    fun `csv missing required column fails on header line`() {
        val csv =
            """
            songId,title,artist,language,originalKeyMidi,lowestMidi,highestMidi,tessituraLowMidi,tessituraHighMidi,rangeSpanSemitones,highNoteBurden,longNoteBurden,leapDifficulty,rhythmDifficulty,overallDifficulty,recommendedKeyShiftMin,recommendedKeyShiftMax,audioUrl,dataSource,credibility,dataVersion,importBatchId
            song-001,晴天,周杰伦,zh,62,57,74,60,69,17,0.35,0.28,0.30,0.40,0.32,-2,3,,人工复核,HIGH,1.0.0,B-001
            """.trimIndent()
        val result = CsvSongParser.parse(csv)
        assertTrue(result.songs.isEmpty())
        assertEquals(1, result.errors.size)
        assertEquals(1, result.errors[0].index, "表头在第 1 行")
        assertTrue(result.errors[0].reason.contains("genre"), result.errors[0].reason)
    }

    @Test
    fun `csv invalid credibility reports line error`() {
        val csv =
            """
            songId,title,artist,language,genre,originalKeyMidi,lowestMidi,highestMidi,tessituraLowMidi,tessituraHighMidi,rangeSpanSemitones,highNoteBurden,longNoteBurden,leapDifficulty,rhythmDifficulty,overallDifficulty,recommendedKeyShiftMin,recommendedKeyShiftMax,audioUrl,dataSource,credibility,dataVersion,importBatchId
            song-001,晴天,周杰伦,zh,流行,62,57,74,60,69,17,0.35,0.28,0.30,0.40,0.32,-2,3,,人工复核,GUESS,1.0.0,B-001
            """.trimIndent()
        val result = CsvSongParser.parse(csv)
        assertEquals(1, result.errors.size)
        assertEquals(2, result.errors[0].index)
        assertTrue(result.errors[0].reason.contains("credibility"), result.errors[0].reason)
    }
}
