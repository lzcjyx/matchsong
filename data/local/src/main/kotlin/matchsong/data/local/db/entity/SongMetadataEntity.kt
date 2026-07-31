package matchsong.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 歌曲元数据 Room 实体（M6.4-1，data-model §2.8 / §3.2 表 `song_metadata`）。
 *
 * 字段与 core:model [matchsong.core.model.song.SongMetadata] 一一对应
 * （映射见 data:local repository 层）：MIDI 音高字段为 Int（歌曲数据整半音精度，
 * FR-SONG-5）；[credibility] 以字符串存储可信度枚举名（"HIGH"/"MEDIUM"/"LOW"，
 * 避免 TypeConverter），导入映射取 [matchsong.core.model.song.Credibility].name。
 *
 * 索引：搜索（title/artist）、筛选（language/genre）与 M7 候选过滤
 * （lowestMidi/highestMidi 音域重叠查询）需要（M6.4-3）。
 */
@Entity(
    tableName = "song_metadata",
    indices = [
        Index("title"),
        Index("artist"),
        Index("language"),
        Index("genre"),
        Index("lowestMidi"),
        Index("highestMidi"),
    ],
)
data class SongMetadataEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val language: String,
    val genre: String,
    /** 原调（MIDI Note，0~127）。 */
    val originalKeyMidi: Int,
    /** 歌曲最低音（MIDI Note，0~127）。 */
    val lowestMidi: Int,
    /** 歌曲最高音（MIDI Note，0~127，≥ lowestMidi，M6.5 校验）。 */
    val highestMidi: Int,
    /** 主要演唱音区下限（MIDI Note，0~127）。 */
    val tessituraLowMidi: Int,
    /** 主要演唱音区上限（MIDI Note，0~127，≥ tessituraLowMidi）。 */
    val tessituraHighMidi: Int,
    /** 音域跨度（半音，= highest − lowest，导入时校验派生）。 */
    val rangeSpanSemitones: Int,
    /** 高音持续负担（0~1）。 */
    val highNoteBurden: Double,
    /** 长音负担（0~1）。 */
    val longNoteBurden: Double,
    /** 跳进难度（0~1）。 */
    val leapDifficulty: Double,
    /** 节奏难度（0~1）。 */
    val rhythmDifficulty: Double,
    /** 总体难度（0~1）。 */
    val overallDifficulty: Double,
    /** 推荐变调范围下限（半音，[-12, 0]，FR-SONG-1）。 */
    val recommendedKeyShiftMin: Int,
    /** 推荐变调范围上限（半音，[0, 12]）。 */
    val recommendedKeyShiftMax: Int,
    /** 试听链接（http(s) 或空，可无试听）。 */
    val audioUrl: String?,
    /** 数据来源声明（FR-SONG-2，M6.5 不得缺失）。 */
    val dataSource: String,
    /** 数据可信度枚举名（"HIGH"/"MEDIUM"/"LOW"，FR-SONG-1）。 */
    val credibility: String,
    /** 歌曲数据版本（语义化版本，FR-SONG-1/M6.4）。 */
    val dataVersion: String,
    /** 导入批次（缺省时由导入方合成）。 */
    val importBatchId: String,
)
