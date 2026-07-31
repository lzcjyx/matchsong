package matchsong.data.songs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * M6.3-2 数据集回归测试（PLAN M6.3 验收：50-200 首、覆盖要求、0 错误门禁）。
 *
 * 每次数据集变更必须通过（防回归）；规模 50-200，语言/风格覆盖，全量校验 0 错误。
 */
class MvpDatasetTest {
    private val datasetFile = File("src/main/resources/songs/mvp-songs.json")

    private fun songs(): List<matchsong.core.model.song.SongMetadata> {
        val parsed = SongDataParser.parse(datasetFile.readText(Charsets.UTF_8))
        assertEquals(0, parsed.errors.size, "解析错误：${parsed.errors.take(3)}")
        return parsed.songs
    }

    @Test
    fun `dataset exists and passes full validation`() {
        assertTrue(datasetFile.exists(), "数据集缺失：${datasetFile.absolutePath}")
        val report = ImportRunner.runImport(datasetFile)
        assertEquals(0, report.failureCount, "数据集必须 0 错误，实际：${report.entryErrors.take(3)}")
        assertTrue(report.successCount in 50..200, "规模应在 50-200，实际 ${report.successCount}")
    }

    @Test
    fun `dataset covers languages and genres`() {
        val list = songs()
        assertTrue(list.any { it.language == "zh" }, "应有中文歌曲")
        assertTrue(list.any { it.language == "en" }, "应有英文歌曲")
        assertTrue(list.map { it.genre }.distinct().size >= 3, "应覆盖 ≥3 风格")
    }

    @Test
    fun `every song has source and credibility`() {
        for (s in songs()) {
            assertTrue(s.dataSource.isNotBlank(), "${s.title} 缺少来源")
            assertTrue(s.credibility.name in setOf("HIGH", "MEDIUM", "LOW"), "${s.title} 可信度非法")
        }
    }

    @Test
    fun `every song range is valid`() {
        for (s in songs()) {
            assertTrue(s.lowestMidi <= s.highestMidi, "${s.title} 最低音>最高音")
            assertTrue(s.tessituraLowMidi >= s.lowestMidi && s.tessituraHighMidi <= s.highestMidi, "${s.title} 主要音区越界")
            assertTrue(s.lowestMidi in 0..127 && s.highestMidi in 0..127, "${s.title} MIDI 越界")
        }
    }
}
