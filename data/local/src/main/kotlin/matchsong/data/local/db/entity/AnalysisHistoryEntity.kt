package matchsong.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分析历史摘要 Room 实体（M8.4-1，data-model §3.2 analysis_history，FR-HX-1）。
 *
 * 只保存分析摘要（时间/音域/舒适区/置信度/算法版本/推荐引用/帧数/质量），
 * 不含原始音频与逐帧轨迹（FR-HX-1/ACC-14）——无 wavPath 等音频字段。
 * [confidenceLevel] 以枚举名（String）存储，映射见 RoomAnalysisHistoryRepository。
 */
@Entity(tableName = "analysis_history")
data class AnalysisHistoryEntity(
    @PrimaryKey val historyId: String,
    /** 记录时间（epoch 毫秒）。 */
    val createdAtMs: Long,
    /** 稳定最低音（MIDI，P5）；样本不足时为 null（FR-ANAL-8）。 */
    val stableLowestMidi: Double?,
    /** 稳定最高音（MIDI，P95）；样本不足时为 null。 */
    val stableHighestMidi: Double?,
    /** 舒适最低音（MIDI）；样本不足时为 null。 */
    val comfortLowestMidi: Double?,
    /** 舒适最高音（MIDI）；样本不足时为 null。 */
    val comfortHighestMidi: Double?,
    /** 置信度分档（ConfidenceLevel.name，SPEC §13）。 */
    val confidenceLevel: String,
    /** 算法版本（FR-ANAL-6/FR-HX-1）。 */
    val algorithmVersion: String,
    /** 推荐引用 JSON（Top 推荐 songId + weightsVersion；null = 无推荐，ACC-9）。 */
    val recommendationRefsJson: String?,
    /** 有效帧数。 */
    val voicedFrameCount: Int,
    /** 质量检测是否可用（FR-QUAL-3）。 */
    val qualityUsable: Boolean,
)
