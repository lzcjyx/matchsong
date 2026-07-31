package matchsong.data.local.repository

import matchsong.core.model.song.SongMetadata
import matchsong.core.model.song.SongRangeProfile
import matchsong.data.local.db.entity.SongMetadataEntity
import matchsong.data.local.db.entity.SongRangeProfileEntity

/**
 * 歌曲模型 → Room 实体映射（M6.4-2）。
 *
 * 解析与校验委托 data:songs（[matchsong.data.songs.SongDataParser] /
 * [matchsong.data.songs.SongImportValidator]，M6.2 契约：裸数组 + 23 个 camelCase 字段）；
 * 本文件仅负责「校验通过 → 落库」的字段映射。
 */

internal fun List<SongMetadata>.toEntities(fallbackBatchId: String): List<SongMetadataEntity> =
    map { song ->
        SongMetadataEntity(
            songId = song.songId,
            title = song.title,
            artist = song.artist,
            language = song.language,
            genre = song.genre,
            originalKeyMidi = song.originalKeyMidi,
            lowestMidi = song.lowestMidi,
            highestMidi = song.highestMidi,
            tessituraLowMidi = song.tessituraLowMidi,
            tessituraHighMidi = song.tessituraHighMidi,
            rangeSpanSemitones = song.rangeSpanSemitones,
            highNoteBurden = song.highNoteBurden,
            longNoteBurden = song.longNoteBurden,
            leapDifficulty = song.leapDifficulty,
            rhythmDifficulty = song.rhythmDifficulty,
            overallDifficulty = song.overallDifficulty,
            recommendedKeyShiftMin = song.recommendedKeyShiftMin,
            recommendedKeyShiftMax = song.recommendedKeyShiftMax,
            audioUrl = song.audioUrl,
            dataSource = song.dataSource,
            credibility = song.credibility.name,
            dataVersion = song.dataVersion,
            importBatchId = song.importBatchId ?: fallbackBatchId,
        )
    }

/**
 * 派生歌曲音域画像（data-model §2.9，导入时冗余存储）。
 *
 * 派生逻辑委托 core:model 的 [SongRangeProfile.from]（唯一事实来源，
 * 避免公式漂移），实体层仅做字段映射。
 */
internal fun List<SongMetadata>.toProfiles(): List<SongRangeProfileEntity> =
    map { song ->
        SongRangeProfile.from(song).toEntity()
    }

/** [SongRangeProfile] → Room 实体（keyShiftRange 拆为 min/max 两列）。 */
internal fun SongRangeProfile.toEntity(): SongRangeProfileEntity =
    SongRangeProfileEntity(
        songId = songId,
        originalRangeLowMidi = originalRangeLowMidi,
        originalRangeHighMidi = originalRangeHighMidi,
        tessituraPosition = tessituraPosition,
        burdenHeadroom = burdenHeadroom,
        keyShiftRangeMin = keyShiftRange.first,
        keyShiftRangeMax = keyShiftRange.last,
        profileVersion = profileVersion,
    )
