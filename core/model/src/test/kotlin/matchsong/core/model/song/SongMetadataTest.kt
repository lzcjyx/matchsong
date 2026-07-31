package matchsong.core.model.song

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * M6.1-1 SongMetadata / SongRangeProfile / Credibility / Genre 测试。
 * 校验为函数而非构造期断言：非法数据必须可构造，且 validate() 返回错误。
 */
class SongMetadataTest {
    private fun validSong() =
        SongMetadata(
            songId = "song-001",
            title = "晴天",
            artist = "周杰伦",
            language = "zh",
            genre = "流行",
            originalKeyMidi = 62,
            lowestMidi = 57,
            highestMidi = 74,
            tessituraLowMidi = 60,
            tessituraHighMidi = 69,
            rangeSpanSemitones = 17,
            highNoteBurden = 0.35,
            longNoteBurden = 0.28,
            leapDifficulty = 0.30,
            rhythmDifficulty = 0.40,
            overallDifficulty = 0.32,
            recommendedKeyShiftMin = -2,
            recommendedKeyShiftMax = 3,
            audioUrl = "https://example.com/qingtian.mp3",
            dataSource = "官方谱面（人工复核）",
            credibility = Credibility.HIGH,
            dataVersion = "1.0.0",
            importBatchId = "B-001",
        )

    @Test
    fun `valid song passes validation`() {
        assertTrue(validSong().validate().isEmpty())
    }

    @Test
    fun `lowest above highest is a validation error not construction failure`() {
        val song = validSong().copy(lowestMidi = 80, highestMidi = 70)
        val errors = song.validate()
        assertTrue(errors.isNotEmpty(), "非法数据应可构造但校验报错")
        assertTrue(errors.any { it.contains("lowestMidi") && it.contains("highestMidi") }, "应指出 lowest>highest：$errors")
    }

    @Test
    fun `midi out of 0_127 fails validation`() {
        assertTrue(validSong().copy(originalKeyMidi = 128).validate().any { it.contains("originalKeyMidi") })
        assertTrue(validSong().copy(lowestMidi = -1).validate().any { it.contains("lowestMidi") })
        assertTrue(validSong().copy(highestMidi = 130).validate().any { it.contains("highestMidi") })
        assertTrue(validSong().copy(tessituraLowMidi = -5).validate().any { it.contains("tessituraLowMidi") })
        assertTrue(validSong().copy(tessituraHighMidi = 128).validate().any { it.contains("tessituraHighMidi") })
    }

    @Test
    fun `burden out of 0_1 fails validation`() {
        val errors = validSong().copy(highNoteBurden = 1.2).validate()
        assertTrue(errors.any { it.contains("highNoteBurden") }, "负担 >1 应报错：$errors")
        assertTrue(validSong().copy(longNoteBurden = -0.1).validate().any { it.contains("longNoteBurden") })
        assertTrue(validSong().copy(leapDifficulty = 1.01).validate().any { it.contains("leapDifficulty") })
        assertTrue(validSong().copy(rhythmDifficulty = 2.0).validate().any { it.contains("rhythmDifficulty") })
        assertTrue(validSong().copy(overallDifficulty = -1.0).validate().any { it.contains("overallDifficulty") })
    }

    @Test
    fun `language genre source and version constraints`() {
        val errors =
            validSong().copy(
                language = "zh-CN",
                genre = "不存在的风格",
                dataSource = "  ",
                dataVersion = "",
            ).validate()
        assertTrue(errors.any { it.contains("language") })
        assertTrue(errors.any { it.contains("genre") })
        assertTrue(errors.any { it.contains("dataSource") })
        assertTrue(errors.any { it.contains("dataVersion") })
    }

    @Test
    fun `tessitura order and key shift range constraints`() {
        assertTrue(
            validSong().copy(tessituraLowMidi = 70, tessituraHighMidi = 60).validate().any {
                it.contains("tessituraLowMidi")
            },
        )
        assertTrue(
            validSong().copy(recommendedKeyShiftMin = -13).validate().any { it.contains("recommendedKeyShiftMin") },
        )
        assertTrue(
            validSong().copy(recommendedKeyShiftMax = 13).validate().any { it.contains("recommendedKeyShiftMax") },
        )
    }

    @Test
    fun `nullable fields default to null`() {
        val song =
            SongMetadata(
                songId = "song-002",
                title = "小幸运",
                artist = "田馥甄",
                language = "zh",
                genre = "流行",
                originalKeyMidi = 64,
                lowestMidi = 55,
                highestMidi = 78,
                tessituraLowMidi = 58,
                tessituraHighMidi = 71,
                rangeSpanSemitones = 23,
                highNoteBurden = 0.45,
                longNoteBurden = 0.50,
                leapDifficulty = 0.40,
                rhythmDifficulty = 0.55,
                overallDifficulty = 0.48,
                recommendedKeyShiftMin = -3,
                recommendedKeyShiftMax = 2,
                dataSource = "公开 MIDI 数据集",
                credibility = Credibility.MEDIUM,
                dataVersion = "1.0.0",
            )
        assertNull(song.audioUrl)
        assertNull(song.importBatchId)
        assertTrue(song.validate().isEmpty())
    }

    @Test
    fun `range profile derivation is correct`() {
        val centered =
            validSong().copy(
                lowestMidi = 60,
                highestMidi = 80,
                tessituraLowMidi = 60,
                tessituraHighMidi = 80,
                highNoteBurden = 0.6,
                longNoteBurden = 0.4,
            )
        val profile = SongRangeProfile.from(centered)
        assertEquals("song-001", profile.songId)
        assertEquals(60, profile.originalRangeLowMidi)
        assertEquals(80, profile.originalRangeHighMidi)
        assertEquals(0.0, profile.tessituraPosition, 1e-9, "主要音区居中 → 0")
        assertEquals(0.4, profile.burdenHeadroom, 1e-9, "1 - max(0.6, 0.4)")
        assertEquals(-2..3, profile.keyShiftRange)
        assertEquals(SongRangeProfile.PROFILE_VERSION, profile.profileVersion)

        val low = centered.copy(tessituraLowMidi = 60, tessituraHighMidi = 64)
        // 主要音区中心 62，音域中心 70，跨度 20 → (62-70)/10 = -0.8（偏低）
        assertEquals(-0.8, SongRangeProfile.from(low).tessituraPosition, 1e-9)

        val zeroSpan =
            validSong().copy(
                lowestMidi = 70,
                highestMidi = 70,
                tessituraLowMidi = 70,
                tessituraHighMidi = 70,
            )
        assertEquals(0.0, SongRangeProfile.from(zeroSpan).tessituraPosition, 1e-9, "零跨度不除零")
    }

    @Test
    fun `genre vocabulary is complete and stable`() {
        assertEquals(12, Genre.ALL.size)
        assertTrue(Genre.isValid("流行"))
        assertTrue(Genre.isValid("R&B"))
        assertTrue(Genre.isValid("其他"))
        assertFalse(Genre.isValid("布鲁斯"))
    }

    @Test
    fun `credibility has the three tiers`() {
        assertEquals(3, Credibility.entries.size)
        assertTrue(Credibility.entries.containsAll(listOf(Credibility.HIGH, Credibility.MEDIUM, Credibility.LOW)))
    }
}
