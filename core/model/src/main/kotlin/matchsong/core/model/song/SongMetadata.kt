package matchsong.core.model.song

import kotlinx.serialization.Serializable

/**
 * 歌曲元数据（FR-SONG-1、PLAN §12.2 全字段、data-model §2.8）。
 *
 * 所有音高字段统一 MIDI Note 内部标准（FR-SONG-5、data-model §1.1），取值 0~127；
 * 变调以半音为单位，正数升调、负数降调。
 * 难度/负担类字段为 0~1 连续值（展示映射到低/中/高在 UI 层处理）。
 * 可空约束遵循 data-model §1.3：ID/歌曲名/最低/最高/原调/语言/来源/版本不可空；
 * 试听链接与导入批次随导入质量可空。
 *
 * 校验采用 [validate] 函数而非构造期断言（M6.1-1 设计决策）：
 * 最低 > 最高等数据问题应在导入期以错误报告呈现，而不是阻止对象构造
 * （task-breakdown M6.1-1「设计为校验函数而非构造期断言」）。
 *
 * @param songId 歌曲唯一 ID（主键，导入时唯一性由 SongImportValidator 检查）
 * @param title 歌曲名，非空、≤ 200 字符
 * @param artist 歌手，非空、≤ 200 字符
 * @param language ISO 639-1 语言码（zh/en/...），小写两字母
 * @param genre 受控风格标签，须在 [Genre.ALL] 内
 * @param originalKeyMidi 原调（MIDI Note，0~127）
 * @param lowestMidi 歌曲最低音（MIDI Note，0~127）
 * @param highestMidi 歌曲最高音（MIDI Note，0~127，须 ≥ lowestMidi）
 * @param tessituraLowMidi 主要音区（主旋律集中区）低端（MIDI Note，0~127）
 * @param tessituraHighMidi 主要音区高端（MIDI Note，0~127，须 ≥ tessituraLowMidi）
 * @param rangeSpanSemitones 音域跨度（半音，= highestMidi − lowestMidi，导入时校验派生一致）
 * @param highNoteBurden 高音持续负担 [0,1]，越高高音越密集
 * @param longNoteBurden 长音负担 [0,1]
 * @param leapDifficulty 跳进难度 [0,1]
 * @param rhythmDifficulty 节奏难度 [0,1]
 * @param overallDifficulty 总体难度 [0,1]
 * @param recommendedKeyShiftMin 推荐变调范围下限（半音，[-12,0]，负数降调）
 * @param recommendedKeyShiftMax 推荐变调范围上限（半音，[0,12]）
 * @param audioUrl 试听或外部链接（http(s) 或 null，可无试听）
 * @param dataSource 数据来源声明，非空（FR-SONG-2）
 * @param credibility 数据可信度（[Credibility]）
 * @param dataVersion 数据版本（语义化版本，导入时校验格式与批次一致）
 * @param importBatchId 导入批次 ID（M6.2 导入工具生成）`[推测]`，可空
 */
@Serializable
data class SongMetadata(
    val songId: String,
    val title: String,
    val artist: String,
    val language: String,
    val genre: String,
    val originalKeyMidi: Int,
    val lowestMidi: Int,
    val highestMidi: Int,
    val tessituraLowMidi: Int,
    val tessituraHighMidi: Int,
    val rangeSpanSemitones: Int,
    val highNoteBurden: Double,
    val longNoteBurden: Double,
    val leapDifficulty: Double,
    val rhythmDifficulty: Double,
    val overallDifficulty: Double,
    val recommendedKeyShiftMin: Int,
    val recommendedKeyShiftMax: Int,
    val audioUrl: String? = null,
    val dataSource: String,
    val credibility: Credibility,
    val dataVersion: String,
    val importBatchId: String? = null,
) {
    /**
     * 单条记录字段级校验（构造期不校验，M6.1-1 决策）。
     *
     * @return 错误消息列表；空列表表示通过。每条错误以字段名开头，便于定位与测试断言。
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (songId.isBlank()) errors += "songId 不能为空"
        if (title.isBlank()) errors += "title 不能为空"
        if (title.length > 200) errors += "title 长度超过 200"
        if (artist.isBlank()) errors += "artist 不能为空"
        if (artist.length > 200) errors += "artist 长度超过 200"
        if (!LANGUAGE_PATTERN.matches(language)) errors += "language 必须是 ISO 639-1 两字母小写码，实际: $language"
        if (!Genre.isValid(genre)) errors += "genre 不在受控词表内: $genre"
        if (originalKeyMidi !in MIDI_RANGE) errors += "originalKeyMidi 超出 MIDI 范围 0..127，实际: $originalKeyMidi"
        if (lowestMidi !in MIDI_RANGE) errors += "lowestMidi 超出 MIDI 范围 0..127，实际: $lowestMidi"
        if (highestMidi !in MIDI_RANGE) errors += "highestMidi 超出 MIDI 范围 0..127，实际: $highestMidi"
        if (lowestMidi > highestMidi) errors += "lowestMidi($lowestMidi) 高于 highestMidi($highestMidi)"
        if (tessituraLowMidi !in MIDI_RANGE) errors += "tessituraLowMidi 超出 MIDI 范围 0..127，实际: $tessituraLowMidi"
        if (tessituraHighMidi !in MIDI_RANGE) errors += "tessituraHighMidi 超出 MIDI 范围 0..127，实际: $tessituraHighMidi"
        if (tessituraLowMidi > tessituraHighMidi) {
            errors += "tessituraLowMidi($tessituraLowMidi) 高于 tessituraHighMidi($tessituraHighMidi)"
        }
        if (rangeSpanSemitones < 0) errors += "rangeSpanSemitones 不能为负，实际: $rangeSpanSemitones"
        if (highNoteBurden !in 0.0..1.0) errors += "highNoteBurden 超出 0..1，实际: $highNoteBurden"
        if (longNoteBurden !in 0.0..1.0) errors += "longNoteBurden 超出 0..1，实际: $longNoteBurden"
        if (leapDifficulty !in 0.0..1.0) errors += "leapDifficulty 超出 0..1，实际: $leapDifficulty"
        if (rhythmDifficulty !in 0.0..1.0) errors += "rhythmDifficulty 超出 0..1，实际: $rhythmDifficulty"
        if (overallDifficulty !in 0.0..1.0) errors += "overallDifficulty 超出 0..1，实际: $overallDifficulty"
        if (recommendedKeyShiftMin !in -12..0) {
            errors += "recommendedKeyShiftMin 须在 [-12,0]（半音），实际: $recommendedKeyShiftMin"
        }
        if (recommendedKeyShiftMax !in 0..12) {
            errors += "recommendedKeyShiftMax 须在 [0,12]（半音），实际: $recommendedKeyShiftMax"
        }
        if (recommendedKeyShiftMin > recommendedKeyShiftMax) {
            errors +=
                "recommendedKeyShiftMin($recommendedKeyShiftMin) 高于 recommendedKeyShiftMax($recommendedKeyShiftMax)"
        }
        if (dataSource.isBlank()) errors += "dataSource 不能为空（FR-SONG-2）"
        if (dataVersion.isBlank()) errors += "dataVersion 不能为空"
        return errors
    }

    private companion object {
        /** MIDI Note 合法范围（data-model §2.8：歌曲数据校验范围 0~127）。 */
        val MIDI_RANGE: IntRange = 0..127

        /** ISO 639-1：小写两字母（data-model §2.8）。 */
        val LANGUAGE_PATTERN: Regex = Regex("^[a-z]{2}$")
    }
}
