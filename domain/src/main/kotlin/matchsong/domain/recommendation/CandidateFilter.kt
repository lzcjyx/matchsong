package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata
import matchsong.domain.analysis.VocalRangeEstimate
import matchsong.domain.port.UserSettings

/**
 * M7.1-1 候选过滤（FR-RECM-1）。
 *
 * 过滤规则顺序（集中定义）：
 * 1. 语言：不在用户语言集合（settings.language + 扩展）内 → 剔除；
 * 2. 排除风格：用户 excludedGenres → 剔除；
 * 3. 数据完整性：缺音域字段（lowest/highest/tessitura）或 credibility=LOW → 剔除
 *    （低可信度数据不得进入正式推荐，FR-RECM-1）；
 * 4. 可调音域：歌曲音域与用户稳定音域差异超出最大可调范围（R-6 ±6 半音）→ 剔除；
 * 5. 性别字段：SongMetadata 无性别字段，天然满足"不以歌手性别硬过滤"。
 *
 * 输出：候选列表 + 过滤原因计数（供 M7.5 降级说明）。
 */
class CandidateFilter(
    private val config: CandidateFilterConfig = CandidateFilterConfig(),
) {
    data class FilterResult(
        val candidates: List<SongMetadata>,
        /** 原因 → 被过滤数量。 */
        val reasons: Map<FilterReason, Int>,
    )

    enum class FilterReason { LANGUAGE, EXCLUDED_GENRE, INCOMPLETE_DATA, LOW_CREDIBILITY, OUT_OF_RANGE }

    fun filter(
        songs: List<SongMetadata>,
        userRange: VocalRangeEstimate?,
        settings: UserSettings,
    ): FilterResult {
        val reasons = mutableMapOf<FilterReason, Int>()
        val candidates =
            songs.filter { song ->
                var keep = true
                // 1. 语言
                if (song.language != settings.language) {
                    reasons[FilterReason.LANGUAGE] = (reasons[FilterReason.LANGUAGE] ?: 0) + 1
                    keep = false
                }
                // 2. 排除风格
                if (keep && song.genre in settings.excludedGenres) {
                    reasons[FilterReason.EXCLUDED_GENRE] = (reasons[FilterReason.EXCLUDED_GENRE] ?: 0) + 1
                    keep = false
                }
                // 3a. 数据完整性
                if (keep && (song.lowestMidi > song.highestMidi || song.tessituraLowMidi > song.tessituraHighMidi)) {
                    reasons[FilterReason.INCOMPLETE_DATA] = (reasons[FilterReason.INCOMPLETE_DATA] ?: 0) + 1
                    keep = false
                }
                // 3b. 可信度（LOW 剔除：数据来源不可靠）
                if (keep && song.credibility.name == "LOW") {
                    reasons[FilterReason.LOW_CREDIBILITY] = (reasons[FilterReason.LOW_CREDIBILITY] ?: 0) + 1
                    keep = false
                }
                // 4. 可调音域（需用户音域）：存在 shift ∈ [-maxShift, +maxShift] 使变调后音域与用户音域有重叠
                val userLowNullable = userRange?.stableLowestMidi
                val userHighNullable = userRange?.stableHighestMidi
                if (keep && userLowNullable != null && userHighNullable != null) {
                    val userLow = userLowNullable
                    val userHigh = userHighNullable
                    val songLow = song.lowestMidi.toDouble()
                    val songHigh = song.highestMidi.toDouble()
                    val tol = config.rangeToleranceSemitones.toDouble()
                    // 变调后与用户音域有重叠的 shift 区间（含容差）
                    val shiftMin = userLow - songHigh - tol
                    val shiftMax = userHigh - songLow + tol
                    val maxShift = config.maxKeyShiftSemitones.toDouble()
                    val canShift = shiftMin <= maxShift && shiftMax >= -maxShift && shiftMin <= shiftMax
                    if (!canShift) {
                        reasons[FilterReason.OUT_OF_RANGE] = (reasons[FilterReason.OUT_OF_RANGE] ?: 0) + 1
                        keep = false
                    }
                }
                keep
            }
        return FilterResult(candidates, reasons)
    }
}

/**
 * M7.1-1 过滤配置（R-6 最大可调 ±6 半音 [推测] 标定）。
 */
data class CandidateFilterConfig(
    /** 最大可调变调范围（半音）。 */
    val maxKeyShiftSemitones: Int = 6,
    /** 音域比较容差（半音，[推测] 2）。 */
    val rangeToleranceSemitones: Int = 2,
)
