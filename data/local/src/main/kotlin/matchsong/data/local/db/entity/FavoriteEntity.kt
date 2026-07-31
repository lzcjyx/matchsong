package matchsong.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 收藏关联表 Room 实体（M6.4-1，data-model §3.2 表 `favorite`，FR-HX-2）。
 *
 * N — N 关联 [SongMetadataEntity]（songId 主键 + 外键，唯一约束天然由主键保证）。
 * 级联策略：删除歌曲时级联删除其收藏记录（CASCADE）——收藏不孤立存在（M6.4-1
 * "删除歌曲级联收藏策略明确"）。
 */
@Entity(
    tableName = "favorite",
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
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    /** 收藏时间（epoch 毫秒）。 */
    val favoritedAtMs: Long,
)
