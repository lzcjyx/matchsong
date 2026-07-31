package matchsong.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 歌曲音域画像 Room 实体（M6.4-1，data-model §2.9 / §3.2 表 `song_range_profile`）。
 *
 * 与 [SongMetadataEntity] 1:1（songId 主键 + 外键）；导入时由 core:model
 * [matchsong.core.model.song.SongRangeProfile].from() 派生（唯一事实来源）并冗余存储，
 * 供 M7.2 变调评估直接消费。删除歌曲时级联删除画像（CASCADE）。
 *
 * 派生语义（同 core:model，`[推测]` 项见其 KDoc）：
 * - tessituraPosition：主要音区在歌曲音域内的相对位置 [-1,1]，0 = 居中；
 * - burdenHeadroom：负担余量 [0,1] = 1 − max(高音负担, 长音负担)；
 * - keyShiftRangeMin/Max：推荐变调范围（半音，keyShiftRange 的 min/max 拆列）。
 */
@Entity(
    tableName = "song_range_profile",
    foreignKeys = [
        ForeignKey(
            entity = SongMetadataEntity::class,
            parentColumns = ["songId"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("songId")],
)
data class SongRangeProfileEntity(
    @PrimaryKey val songId: String,
    /** 原调最低音（MIDI Note，= 元数据 lowestMidi）。 */
    val originalRangeLowMidi: Int,
    /** 原调最高音（MIDI Note，= 元数据 highestMidi）。 */
    val originalRangeHighMidi: Int,
    /** 主要演唱音区在歌曲音域内的相对位置（[-1,1]，派生）。 */
    val tessituraPosition: Double,
    /** 负担余量（[0,1]，= 1 − 最高负担指标，派生）。 */
    val burdenHeadroom: Double,
    /** 推荐变调范围下限（半音，[-12, 0]）。 */
    val keyShiftRangeMin: Int,
    /** 推荐变调范围上限（半音，[0, 12]）。 */
    val keyShiftRangeMax: Int,
    /** 画像派生逻辑版本（语义化版本）。 */
    val profileVersion: String,
)
