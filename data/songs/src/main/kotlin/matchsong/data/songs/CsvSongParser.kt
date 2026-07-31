package matchsong.data.songs

import matchsong.core.model.song.Credibility
import matchsong.core.model.song.SongMetadata

/**
 * 歌曲数据 CSV 解析器（M6.2-1）。
 *
 * 列头映射（固定顺序，表头即字段名，与 JSON 键 / [SongMetadata] 字段一致）：
 * ```
 * songId,title,artist,language,genre,originalKeyMidi,lowestMidi,highestMidi,
 * tessituraLowMidi,tessituraHighMidi,rangeSpanSemitones,highNoteBurden,longNoteBurden,
 * leapDifficulty,rhythmDifficulty,overallDifficulty,recommendedKeyShiftMin,
 * recommendedKeyShiftMax,audioUrl,dataSource,credibility,dataVersion,importBatchId
 * ```
 *
 * 规则：
 * - UTF-8 BOM：文件开头与表头单元格的 `\uFEFF` 自动剥离；
 * - 引号转义（RFC 4180）：字段可被双引号包裹，内含逗号/换行，双引号用 `""` 转义；
 * - 空 `audioUrl` / `importBatchId` 单元格 → null；其余字段为空或类型错误 → 该行错误；
 * - 空行跳过；行号语义：表头为第 1 行，首条数据为第 2 行，[EntryError.index] 即文件行号。
 */
object CsvSongParser {
    /** 列头字段顺序（表头即字段名）。 */
    val HEADER: List<String> =
        listOf(
            "songId", "title", "artist", "language", "genre",
            "originalKeyMidi", "lowestMidi", "highestMidi", "tessituraLowMidi", "tessituraHighMidi",
            "rangeSpanSemitones", "highNoteBurden", "longNoteBurden", "leapDifficulty", "rhythmDifficulty",
            "overallDifficulty", "recommendedKeyShiftMin", "recommendedKeyShiftMax",
            "audioUrl", "dataSource", "credibility", "dataVersion", "importBatchId",
        )

    /** 解析 [csvText] 为歌曲列表 + 条目级错误（错误行按行号定位）。 */
    fun parse(csvText: String): ParserResult {
        val rows = tokenize(csvText.removePrefix("\uFEFF"))
        if (rows.isEmpty()) {
            return ParserResult(emptyList(), listOf(EntryError(1, "文件为空")))
        }
        val header = rows[0].map { it.removePrefix("\uFEFF") }
        val missingColumns = HEADER.filter { it !in header }
        if (missingColumns.isNotEmpty()) {
            return ParserResult(
                emptyList(),
                listOf(EntryError(1, "表头缺少必需列：${missingColumns.joinToString(", ")}")),
            )
        }
        val columnIndex = HEADER.associateWith { name -> header.indexOf(name) }

        val songs = mutableListOf<SongMetadata>()
        val errors = mutableListOf<EntryError>()
        for (rowIndex in 1 until rows.size) {
            val cells = rows[rowIndex]
            if (cells.all { it.isBlank() }) continue
            val lineNumber = rowIndex + 1 // 文件行号：表头第 1 行，数据从第 2 行起
            try {
                songs += buildSong(cells, columnIndex)
            } catch (e: CsvCellException) {
                errors += EntryError(lineNumber, "第 $lineNumber 行解析失败：${e.message}")
            }
        }
        return ParserResult(songs, errors)
    }

    /** 从单元格构造 [SongMetadata]；单元格缺失或无法转换时抛 [CsvCellException]。 */
    private fun buildSong(
        cells: List<String>,
        columnIndex: Map<String, Int>,
    ): SongMetadata {
        fun cell(name: String): String {
            val index = columnIndex.getValue(name)
            return if (index < cells.size) cells[index].trim() else ""
        }

        fun intCell(name: String): Int =
            cell(name).toIntOrNull()
                ?: throw CsvCellException("字段 $name 无法解析为整数，实际值: '${cell(name)}'")

        fun doubleCell(name: String): Double =
            cell(name).toDoubleOrNull()
                ?: throw CsvCellException("字段 $name 无法解析为数值，实际值: '${cell(name)}'")

        fun nullableCell(name: String): String? = cell(name).ifEmpty { null }

        fun credibilityCell(name: String): Credibility =
            Credibility.entries.firstOrNull { it.name == cell(name) }
                ?: throw CsvCellException("字段 $name 不是合法可信度（HIGH/MEDIUM/LOW），实际值: '${cell(name)}'")

        return SongMetadata(
            songId = cell("songId"),
            title = cell("title"),
            artist = cell("artist"),
            language = cell("language"),
            genre = cell("genre"),
            originalKeyMidi = intCell("originalKeyMidi"),
            lowestMidi = intCell("lowestMidi"),
            highestMidi = intCell("highestMidi"),
            tessituraLowMidi = intCell("tessituraLowMidi"),
            tessituraHighMidi = intCell("tessituraHighMidi"),
            rangeSpanSemitones = intCell("rangeSpanSemitones"),
            highNoteBurden = doubleCell("highNoteBurden"),
            longNoteBurden = doubleCell("longNoteBurden"),
            leapDifficulty = doubleCell("leapDifficulty"),
            rhythmDifficulty = doubleCell("rhythmDifficulty"),
            overallDifficulty = doubleCell("overallDifficulty"),
            recommendedKeyShiftMin = intCell("recommendedKeyShiftMin"),
            recommendedKeyShiftMax = intCell("recommendedKeyShiftMax"),
            audioUrl = nullableCell("audioUrl"),
            dataSource = cell("dataSource"),
            credibility = credibilityCell("credibility"),
            dataVersion = cell("dataVersion"),
            importBatchId = nullableCell("importBatchId"),
        )
    }

    /**
     * RFC 4180 风格 CSV 分词：支持双引号包裹（含逗号/换行）与 `""` 转义。
     * 返回行列表；全空行保留给调用方过滤。
     */
    private fun tokenize(text: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes ->
                    when {
                        char == '"' -> {
                            if (index + 1 < text.length && text[index + 1] == '"') {
                                cell.append('"')
                                index++
                            } else {
                                inQuotes = false
                            }
                        }
                        else -> cell.append(char)
                    }
                char == '"' -> inQuotes = true
                char == ',' -> {
                    row.add(cell.toString())
                    cell.clear()
                }
                char == '\r' || char == '\n' -> {
                    if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row.add(cell.toString())
                    cell.clear()
                    rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(char)
            }
            index++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
}

/** CSV 单元格级转换失败（内部类型，携带中文原因）。 */
private class CsvCellException(message: String) : Exception(message)
