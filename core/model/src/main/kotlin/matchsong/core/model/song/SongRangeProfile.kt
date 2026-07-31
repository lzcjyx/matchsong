package matchsong.core.model.song

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

/**
 * 歌曲音域画像（data-model §2.9）：供变调评估（M7.2/FR-RECM-2）直接消费的派生画像。
 *
 * 导入时由 [SongMetadata] 派生并冗余存储，避免推荐期重复计算（同一歌曲 × 变调组合
 * 的中间量在 M7.2 内存中计算、不持久化）。所有音高字段 MIDI Note 内部标准（FR-SONG-5）。
 *
 * @param songId 歌曲 ID，与 [SongMetadata.songId] 一一对应
 * @param originalRangeLowMidi 原调最低音（= lowestMidi）
 * @param originalRangeHighMidi 原调最高音（= highestMidi）
 * @param tessituraPosition 主要音区在歌曲音域内的相对位置 [-1,1] `[推测]`：0 = 居中，
 *   负 = 主要音区整体偏低，正 = 整体偏高
 * @param burdenHeadroom 负担余量 [0,1] `[推测]`：1 − max(高音负担, 长音负担)，
 *   越大表示对演唱者越宽松
 * @param keyShiftRange 推荐变调范围（半音，含端点），由 recommendedKeyShiftMin..Max 组合
 * @param profileVersion 画像派生逻辑版本（语义化版本，派生逻辑变更时递增）
 */
@Serializable
data class SongRangeProfile(
    val songId: String,
    val originalRangeLowMidi: Int,
    val originalRangeHighMidi: Int,
    val tessituraPosition: Double,
    val burdenHeadroom: Double,
    @Serializable(with = IntRangeSerializer::class)
    val keyShiftRange: IntRange,
    val profileVersion: String,
) {
    companion object {
        /** 当前画像派生逻辑版本（M6.1-1）。 */
        const val PROFILE_VERSION: String = "1.0.0"

        /**
         * 由 [song] 派生画像（data-model §2.9）。
         *
         * @param song 已通过字段校验的歌曲元数据
         */
        fun from(song: SongMetadata): SongRangeProfile {
            val rangeSpan = song.highestMidi - song.lowestMidi
            val tessituraCenter = (song.tessituraLowMidi + song.tessituraHighMidi) / 2.0
            val rangeCenter = (song.lowestMidi + song.highestMidi) / 2.0
            val position =
                if (rangeSpan > 0) {
                    ((tessituraCenter - rangeCenter) / (rangeSpan / 2.0)).coerceIn(-1.0, 1.0)
                } else {
                    0.0
                }
            val maxBurden = maxOf(song.highNoteBurden, song.longNoteBurden)
            return SongRangeProfile(
                songId = song.songId,
                originalRangeLowMidi = song.lowestMidi,
                originalRangeHighMidi = song.highestMidi,
                tessituraPosition = position,
                burdenHeadroom = (1.0 - maxBurden).coerceIn(0.0, 1.0),
                keyShiftRange = song.recommendedKeyShiftMin..song.recommendedKeyShiftMax,
                profileVersion = PROFILE_VERSION,
            )
        }
    }
}

/**
 * [IntRange] 的序列化器：编码为 `{"min": .., "max": ..}` 结构。
 * kotlinx.serialization 未内置 IntRange 序列化，故自定义（仅供 SongRangeProfile 使用）。
 */
private object IntRangeSerializer : KSerializer<IntRange> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("SongRangeProfile.keyShiftRange") {
            element<Int>("min")
            element<Int>("max")
        }

    override fun serialize(
        encoder: Encoder,
        value: IntRange,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.first)
            encodeIntElement(descriptor, 1, value.last)
        }
    }

    override fun deserialize(decoder: Decoder): IntRange {
        val input = decoder.beginStructure(descriptor)
        var min = 0
        var max = 0
        while (true) {
            when (val index = input.decodeElementIndex(descriptor)) {
                0 -> min = input.decodeIntElement(descriptor, 0)
                1 -> max = input.decodeIntElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        input.endStructure(descriptor)
        return min..max
    }
}
