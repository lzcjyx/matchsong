package matchsong.data.songs

import matchsong.core.model.song.Credibility
import matchsong.core.model.song.SongMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M6.2-2 SongImportValidator 测试：重复 ID / 无效音高 / 最低>最高 / 缺失来源 / 版本格式错误等。
 */
class SongImportValidatorTest {
    private fun song(
        songId: String = "song-001",
        title: String = "晴天",
        artist: String = "周杰伦",
        dataVersion: String = "1.0.0",
        originalKeyMidi: Int = 62,
        lowestMidi: Int = 57,
        highestMidi: Int = 74,
        rangeSpanSemitones: Int = 17,
        dataSource: String = "官方谱面（人工复核）",
    ) = SongMetadata(
        songId = songId,
        title = title,
        artist = artist,
        language = "zh",
        genre = "流行",
        originalKeyMidi = originalKeyMidi,
        lowestMidi = lowestMidi,
        highestMidi = highestMidi,
        tessituraLowMidi = 60,
        tessituraHighMidi = 69,
        rangeSpanSemitones = rangeSpanSemitones,
        highNoteBurden = 0.35,
        longNoteBurden = 0.28,
        leapDifficulty = 0.30,
        rhythmDifficulty = 0.40,
        overallDifficulty = 0.32,
        recommendedKeyShiftMin = -2,
        recommendedKeyShiftMax = 3,
        dataSource = dataSource,
        credibility = Credibility.HIGH,
        dataVersion = dataVersion,
    )

    @Test
    fun `valid list passes`() {
        val result =
            SongImportValidator.validate(
                listOf(song(), song(songId = "song-002", title = "小幸运", artist = "田馥甄")),
            )
        assertTrue(result.isValid, "合法列表应通过：${result.entryErrors}")
    }

    @Test
    fun `duplicate songId fails`() {
        val result = SongImportValidator.validate(listOf(song(), song(songId = "song-001")))
        assertFalse(result.isValid)
        assertEquals(1, result.entryErrors.size)
        assertEquals(1, result.entryErrors[0].index, "第二条为重复")
        assertTrue(result.entryErrors[0].reason.contains("songId 重复"), result.entryErrors[0].reason)
    }

    @Test
    fun `exact duplicate title artist version fails even with different ids`() {
        val result = SongImportValidator.validate(listOf(song(), song(songId = "song-999")))
        assertFalse(result.isValid)
        assertTrue(result.entryErrors[0].reason.contains("title+artist+dataVersion 精确重复"), result.entryErrors[0].reason)
    }

    @Test
    fun `same song different version is not an exact duplicate`() {
        // 同歌不同版本（不同 dataVersion + 不同 songId）：不触发 title+artist+dataVersion 精确重复
        // 错误；但单批次（单文件）内混用版本触发批次一致性错误，跨批次导入才允许不同版本（M6.2-2 策略）
        val result =
            SongImportValidator.validate(
                listOf(song(), song(songId = "song-001-v2", dataVersion = "2.0.0")),
            )
        assertFalse(result.isValid)
        val reason = result.entryErrors[0].reason
        assertTrue(reason.contains("批次不一致"), reason)
        assertFalse(reason.contains("精确重复"), "不同版本不应触发精确重复错误：$reason")
    }

    @Test
    fun `original key outside range passes`() {
        // M6.3-2 语义修正：originalKey 是歌曲调性（伴奏），独立于演唱音域；
        // 原调高于/低于音域合法（变调推荐正是为此设计）
        val result = SongImportValidator.validate(listOf(song(originalKeyMidi = 100)))
        assertTrue(result.isValid, result.entryErrors.toString())
    }

    @Test
    fun `lowest above highest fails`() {
        val result = SongImportValidator.validate(listOf(song(lowestMidi = 80, highestMidi = 70)))
        assertFalse(result.isValid)
        assertTrue(result.entryErrors[0].reason.contains("lowestMidi"), result.entryErrors[0].reason)
    }

    @Test
    fun `derived span mismatch fails`() {
        // rangeSpanSemitones(10) ≠ highest(74) − lowest(57)=17
        val result = SongImportValidator.validate(listOf(song(rangeSpanSemitones = 10)))
        assertFalse(result.isValid)
        assertTrue(result.entryErrors[0].reason.contains("rangeSpanSemitones"), result.entryErrors[0].reason)
    }

    @Test
    fun `blank data source fails`() {
        val result = SongImportValidator.validate(listOf(song(dataSource = "   ")))
        assertFalse(result.isValid)
        assertTrue(result.entryErrors[0].reason.contains("dataSource"), result.entryErrors[0].reason)
    }

    @Test
    fun `invalid data version format fails`() {
        val result = SongImportValidator.validate(listOf(song(dataVersion = "abc")))
        assertFalse(result.isValid)
        assertTrue(result.entryErrors[0].reason.contains("dataVersion"), result.entryErrors[0].reason)
    }

    @Test
    fun `mixed batch versions fail with batch inconsistency`() {
        val result =
            SongImportValidator.validate(
                listOf(song(), song(songId = "song-002", title = "小幸运", artist = "田馥甄", dataVersion = "2.0.0")),
            )
        assertFalse(result.isValid)
        assertEquals(1, result.entryErrors.size)
        assertTrue(result.entryErrors[0].reason.contains("批次不一致"), result.entryErrors[0].reason)
    }

    @Test
    fun `semver prerelease suffix is accepted`() {
        val result =
            SongImportValidator.validate(
                listOf(
                    song(dataVersion = "1.0.0-beta.1"),
                    song(songId = "song-002", title = "小幸运", artist = "田馥甄", dataVersion = "1.0.0-beta.1"),
                ),
            )
        assertTrue(result.isValid, result.entryErrors.toString())
    }

    @Test
    fun `midi out of range fails`() {
        val result = SongImportValidator.validate(listOf(song(lowestMidi = -1)))
        assertFalse(result.isValid)
        assertTrue(result.entryErrors[0].reason.contains("lowestMidi"), result.entryErrors[0].reason)
    }

    @Test
    fun `multiple failures aggregate into one entry error`() {
        // 最低>最高 + 跨度不一致 + 版本格式错误 → 单条 EntryError 多个原因
        val result =
            SongImportValidator.validate(
                listOf(song(lowestMidi = 80, highestMidi = 70, rangeSpanSemitones = 10, dataVersion = "x")),
            )
        assertEquals(1, result.entryErrors.size)
        val reason = result.entryErrors[0].reason
        assertTrue(reason.contains("lowestMidi"), reason)
        assertTrue(reason.contains("rangeSpanSemitones"), reason)
        assertTrue(reason.contains("dataVersion"), reason)
    }
}
