package matchsong.domain.recommendation

import matchsong.core.model.song.SongMetadata
import matchsong.domain.analysis.VocalRangeEstimate

/**
 * M7.2-1 变调评估（FR-RECM-2，ACC-17）。
 *
 * 对每首候选在 [-maxShift, +maxShift] 枚举半音偏移：
 * - 变调后最低/最高音 = 原音域 + shift；
 * - 匹配判定：变调后最高音 ≤ 用户稳定最高音 + 容差，且最低音 ≥ 用户稳定最低音 − 容差；
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

        // 优先降调（负偏移从小到大枚举 |shift|）：降调更符合听感（升调通常更吃力）
        val shifts =
            (-config.maxKeyShiftSemitones..0).sortedBy { kotlin.math.abs(it) } +
                (1..config.maxKeyShiftSemitones)
        for (shift in shifts) {
            val low = song.lowestMidi + shift
            val high = song.highestMidi + shift
            val fits =
                high <= userHigh + config.toleranceSemitones &&
                    low >= userLow - config.toleranceSemitones
            if (fits) {
                return KeyShiftResult(shift, low, high, inRange = true)
            }
        }
        // 原调已在范围内（shift=0 应已被上面捕获；防御）
        val low0 = song.lowestMidi
        val high0 = song.highestMidi
        return if (high0 <= userHigh + config.toleranceSemitones && low0 >= userLow - config.toleranceSemitones) {
            KeyShiftResult(0, low0, high0, inRange = true)
        } else {
            KeyShiftResult(null, low0, high0, inRange = false)
        }
    }
}

/**
 * M7.2-1 变调配置（R-6 ±6 半音，容差 [推测] 2 半音）。
 */
data class KeyShiftConfig(
    val maxKeyShiftSemitones: Int = 6,
    val toleranceSemitones: Int = 2,
)
