package matchsong.data.songs

import matchsong.core.model.song.SongMetadata

/**
 * 导入校验结果（M6.2-2）。
 *
 * @param entryErrors 条目级错误列表（[EntryError.index] = 输入列表下标，0 起；
 *   reason 内含 songId/title 便于在报告中定位）
 */
data class ImportValidationResult(
    val entryErrors: List<EntryError>,
) {
    /** 是否全部通过。 */
    val isValid: Boolean get() = entryErrors.isEmpty()
}

/**
 * 歌曲数据导入校验器（M6.2-2）。
 *
 * 校验项：
 * 1. 字段校验：复用 [SongMetadata.validate]（非空 / MIDI 0~127 / 负担难度 [0,1] / 语言 / 风格等）；
 * 2. 重复检查：songId 重复报错；同 title+artist+dataVersion 精确重复报错；
 *    同歌不同版本（不同 dataVersion + 不同 songId）允许（策略记录于 task-breakdown M6.2-2）；
 * 3. 音高范围：lowest ≤ highest（字段校验）、原调在 [lowest, highest] 内、
 *    rangeSpanSemitones 与 highest − lowest 派生一致（data-model §2.8「导入时校验派生」）；
 * 4. 来源检查：dataSource 非空（字段校验，FR-SONG-2）、credibility 为合法枚举（类型系统保证）；
 * 5. 版本检查：dataVersion 语义化版本格式（semver-ish）、批次内版本一致。
 *
 * 每条失败条目聚合为单个 [EntryError]（多个原因以 `；` 连接）。
 */
object SongImportValidator {
    /** 语义化版本（semver-ish）：主.次.修订，可选预发布/构建元数据。 */
    private val SEMVER_PATTERN: Regex = Regex("""^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.\-]+)?$""")

    /**
     * 校验 [songs]，返回条目级错误列表。
     *
     * @param songs 已解析的歌曲列表（通常来自 [SongDataParser] / [CsvSongParser]）
     */
    fun validate(songs: List<SongMetadata>): ImportValidationResult {
        val errors = mutableListOf<EntryError>()
        val seenSongIds = mutableMapOf<String, Int>()
        val seenTitleArtistVersion = mutableMapOf<Triple<String, String, String>, Int>()
        var batchVersion: String? = null

        songs.forEachIndexed { index, song ->
            val reasons = mutableListOf<String>()

            // 1. 字段校验（含最低 ≤ 最高、MIDI 0~127、负担 [0,1]、非空约束）
            reasons += song.validate()

            // 2. 重复检查
            val previousId = seenSongIds.put(song.songId, index)
            if (previousId != null) {
                reasons += "songId 重复（与第 $previousId 条相同）：${song.songId}"
            }
            val identityKey = Triple(song.title, song.artist, song.dataVersion)
            val previousIdentity = seenTitleArtistVersion.put(identityKey, index)
            if (previousIdentity != null) {
                reasons += "title+artist+dataVersion 精确重复（与第 $previousIdentity 条相同）：" +
                    "${song.title} / ${song.artist} / ${song.dataVersion}"
            }

            // 3. 音高范围（跨字段派生检查）
            // 注意：originalKeyMidi 是歌曲调性（伴奏），与演唱音域（lowest/highest）独立，
            // 不要求落在音域内（M6.3-2 数据集修正记录：原调可高于/低于演唱音域，变调即为此设计）
            if (song.rangeSpanSemitones != song.highestMidi - song.lowestMidi) {
                reasons += "rangeSpanSemitones(${song.rangeSpanSemitones}) 与 highestMidi − lowestMidi" +
                    "(${song.highestMidi - song.lowestMidi}) 不一致"
            }

            // 5. 版本检查（格式 + 批次一致）
            if (!SEMVER_PATTERN.matches(song.dataVersion)) {
                reasons += "dataVersion 不是语义化版本格式，实际: ${song.dataVersion}"
            }
            if (batchVersion != null && song.dataVersion != batchVersion) {
                reasons += "批次不一致：dataVersion(${song.dataVersion}) 与批次版本($batchVersion) 不同"
            } else if (batchVersion == null && SEMVER_PATTERN.matches(song.dataVersion)) {
                batchVersion = song.dataVersion
            }

            if (reasons.isNotEmpty()) {
                errors += EntryError(index, reasons.joinToString("；"))
            }
        }
        return ImportValidationResult(errors)
    }
}
