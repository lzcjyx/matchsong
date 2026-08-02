package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata
import matchsong.domain.analysis.VocalRangeEstimate
import kotlin.math.max
import kotlin.math.min

/**
 * M7.2-1 变调评估（FR-RECM-2，ACC-17）。
 *
 * 对每首候选在 [-maxShift, +maxShift] 枚举半音偏移：
 * - 变调后最低/最高音 = 原音域 + shift；
 * - 匹配判定（BUG-023 修复：原"全包含"规则在偏女调数据集上把典型男声全部滤掉）：
 *   ① 变调后音域与用户音域的**重叠比例 ≥ [minOverlapRatio]**（歌曲跨度占比）；
 *   ② 变调后最高音 ≤ 用户稳定最高音 + 容差 + [maxExcessSemitones]（上限超幅受控）；
 *   ③ 变调后最低音 ≥ 用户稳定最低音 − 容差 − [maxExcessSemitones]。
 *   全包含（原规则）等价于重叠比例=1 且超幅=0，是其特例；
 * - 选最优偏移：优先降调（shift ≤ 0 中满足条件的最小 |shift|），无降调可行才升调；
 * - 不可调 → keyShiftSemitones = null。
 */
class KeyShiftEvaluation(
    private val config: KeyShiftConfig = KeyShiftConfig(),
) {
    data class KeyShiftResult(
        /** 最优半音偏移（负=降调）；null=不可调。 */
        val keyShiftSemitones: Int?,
        /** 变调后最低音（MIDI）。 */
        val transposedLowestMidi: Int,
        /** 变调后最高音（MIDI）。 */
        val transposedHighestMidi: Int,
        /** 是否在用户可调范围内。 */
        val inRange: Boolean,
    )

    fun evaluate(
        song: SongMetadata,
        userRange: VocalRangeEstimate,
    ): KeyShiftResult {
        val userLow =
            userRange.stableLowestMidi
                ?: return KeyShiftResult(null, song.lowestMidi, song.highestMidi, false)
        val userHigh =
            userRange.stableHighestMidi
                ?: return KeyShiftResult(null, song.lowestMidi, song.highestMidi, false)

        val songSpan = song.highestMidi - song.lowestMidi
        val lowerBound = userLow - config.toleranceSemitones - config.maxExcessSemitones
        val upperBound = userHigh + config.toleranceSemitones + config.maxExcessSemitones

        // 优先降调（负偏移从小到大枚举 |shift|）：降调更符合听感（升调通常更吃力）
        val shifts =
            (-config.maxKeyShiftSemitones..0).sortedBy { kotlin.math.abs(it) } +
                (1..config.maxKeyShiftSemitones)

        // Pass 1：零超幅匹配（变调后完全落入用户音域±容差；与旧规则一致，ACC-17）
        for (shift in shifts) {
            val low = song.lowestMidi + shift
            val high = song.highestMidi + shift
            if (high <= userHigh + config.toleranceSemitones &&
                low >= userLow - config.toleranceSemitones
            ) {
                return KeyShiftResult(shift, low, high, inRange = true)
            }
        }

        // Pass 2（BUG-023）：部分重叠匹配——变调后音域与用户音域重叠比例 ≥ minOverlapRatio
        // 且超幅 ≤ maxExcessSemitones（偏女调数据集下典型男声需此回退，否则推荐全空）
        for (shift in shifts) {
            val low = song.lowestMidi + shift
            val high = song.highestMidi + shift
            if (high > upperBound || low < lowerBound) continue
            val overlap =
                max(
                    0.0,
                    min(high.toDouble(), userHigh + config.toleranceSemitones) -
                        max(low.toDouble(), userLow - config.toleranceSemitones),
                )
            val overlapRatio =
                if (songSpan > 0) overlap / songSpan else 1.0
            if (overlapRatio >= config.minOverlapRatio) {
                return KeyShiftResult(shift, low, high, inRange = true)
            }
        }
        return KeyShiftResult(null, song.lowestMidi, song.highestMidi, inRange = false)
    }
}

/**
 * M7.2-1 变调配置（R-6 ±6 半音；BUG-023 部分重叠规则 [推测]，真机标定）。
 */
data class KeyShiftConfig(
    val maxKeyShiftSemitones: Int = 6,
    val toleranceSemitones: Int = 2,
    /** 变调后与用户音域的重叠比例下限（歌曲跨度占比，[推测] 0.6）。 */
    val minOverlapRatio: Double = 0.6,
    /** 变调后超出用户音域（容差外）的最大半音数（[推测] 4）。 */
    val maxExcessSemitones: Int = 4,
)
