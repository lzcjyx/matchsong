package matchsong.data.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import matchsong.core.model.song.Credibility
import matchsong.core.model.song.SongMetadata
import matchsong.data.local.db.entity.AnalysisHistoryEntity
import matchsong.data.local.db.entity.SongMetadataEntity
import matchsong.domain.analysis.ConfidenceLevel

/** 测试用历史摘要实体工厂（字段默认值覆盖 M8.4 常规场景）。 */
internal fun analysisHistoryEntity(
    historyId: String = "history-1",
    createdAtMs: Long = 1_000,
    stableLowestMidi: Double? = 48.0,
    stableHighestMidi: Double? = 69.0,
    comfortLowestMidi: Double? = 52.0,
    comfortHighestMidi: Double? = 64.0,
    confidenceLevel: String = ConfidenceLevel.HIGH.name,
    algorithmVersion: String = "1.0.0",
    recommendationRefsJson: String? = null,
    voicedFrameCount: Int = 500,
    qualityUsable: Boolean = true,
): AnalysisHistoryEntity =
    AnalysisHistoryEntity(
        historyId = historyId,
        createdAtMs = createdAtMs,
        stableLowestMidi = stableLowestMidi,
        stableHighestMidi = stableHighestMidi,
        comfortLowestMidi = comfortLowestMidi,
        comfortHighestMidi = comfortHighestMidi,
        confidenceLevel = confidenceLevel,
        algorithmVersion = algorithmVersion,
        recommendationRefsJson = recommendationRefsJson,
        voicedFrameCount = voicedFrameCount,
        qualityUsable = qualityUsable,
    )

/** 测试用歌曲实体工厂（字段默认值覆盖 M6.4 测试常规场景）。 */
internal fun songEntity(
    songId: String = "song-1",
    title: String = "测试歌曲",
    artist: String = "测试歌手",
    language: String = "zh",
    genre: String = "流行",
    originalKeyMidi: Int = 60,
    lowestMidi: Int = 55,
    highestMidi: Int = 72,
    tessituraLowMidi: Int = 57,
    tessituraHighMidi: Int = 69,
    rangeSpanSemitones: Int = 17,
    highNoteBurden: Double = 0.3,
    longNoteBurden: Double = 0.2,
    leapDifficulty: Double = 0.4,
    rhythmDifficulty: Double = 0.5,
    overallDifficulty: Double = 0.4,
    recommendedKeyShiftMin: Int = -4,
    recommendedKeyShiftMax: Int = 3,
    audioUrl: String? = "https://example.com/audio.mp3",
    dataSource: String = "test-dataset",
    credibility: String = Credibility.HIGH.name,
    dataVersion: String = "1.0.0",
    importBatchId: String = "batch-1",
): SongMetadataEntity =
    SongMetadataEntity(
        songId = songId,
        title = title,
        artist = artist,
        language = language,
        genre = genre,
        originalKeyMidi = originalKeyMidi,
        lowestMidi = lowestMidi,
        highestMidi = highestMidi,
        tessituraLowMidi = tessituraLowMidi,
        tessituraHighMidi = tessituraHighMidi,
        rangeSpanSemitones = rangeSpanSemitones,
        highNoteBurden = highNoteBurden,
        longNoteBurden = longNoteBurden,
        leapDifficulty = leapDifficulty,
        rhythmDifficulty = rhythmDifficulty,
        overallDifficulty = overallDifficulty,
        recommendedKeyShiftMin = recommendedKeyShiftMin,
        recommendedKeyShiftMax = recommendedKeyShiftMax,
        audioUrl = audioUrl,
        dataSource = dataSource,
        credibility = credibility,
        dataVersion = dataVersion,
        importBatchId = importBatchId,
    )

/** 测试用目录 JSON 条目（裸数组契约，与 data:songs song-schema.json 一致）。
 *
 * title/artist 缺省时按 songId 派生，保证条目身份唯一（SongImportValidator
 * 拒绝 title+artist+dataVersion 精确重复）。
 */
internal fun songJson(
    songId: String = "song-1",
    title: String = "",
    artist: String = "",
    language: String = "zh",
    genre: String = "流行",
    lowestMidi: Int = 55,
    highestMidi: Int = 72,
    credibility: Credibility = Credibility.HIGH,
    dataVersion: String = "1.0.0",
    importBatchId: String? = "batch-1",
    audioUrl: String? = null,
): SongMetadata =
    SongMetadata(
        songId = songId,
        title = title.ifBlank { "歌曲 $songId" },
        artist = artist.ifBlank { "歌手 $songId" },
        language = language,
        genre = genre,
        originalKeyMidi = 60,
        lowestMidi = lowestMidi,
        highestMidi = highestMidi,
        tessituraLowMidi = 57,
        tessituraHighMidi = 69,
        rangeSpanSemitones = highestMidi - lowestMidi,
        highNoteBurden = 0.3,
        longNoteBurden = 0.2,
        leapDifficulty = 0.4,
        rhythmDifficulty = 0.5,
        overallDifficulty = 0.4,
        recommendedKeyShiftMin = -4,
        recommendedKeyShiftMax = 3,
        audioUrl = audioUrl,
        dataSource = "test-dataset",
        credibility = credibility,
        dataVersion = dataVersion,
        importBatchId = importBatchId,
    )

/**
 * 将目录模型序列化为导入用的 JSON 字符串（顶层裸数组）。
 *
 * 手动构造 JsonObject（而非依赖 SongMetadata 的编译期 serializer()——其 companion 为
 * private，跨模块编译期解析不可用；运行时反序列化查找不受影响，见 SongImportRepository
 * 委托的 data:songs SongDataParser）。
 * 可空字段（audioUrl/importBatchId）为 null 时省略，模拟真实数据集契约。
 */
internal fun catalogJson(songs: List<SongMetadata>): String {
    val json = Json
    val objects: List<JsonObject> =
        songs.map { song ->
            buildJsonObject {
                put("songId", song.songId)
                put("title", song.title)
                put("artist", song.artist)
                put("language", song.language)
                put("genre", song.genre)
                put("originalKeyMidi", song.originalKeyMidi)
                put("lowestMidi", song.lowestMidi)
                put("highestMidi", song.highestMidi)
                put("tessituraLowMidi", song.tessituraLowMidi)
                put("tessituraHighMidi", song.tessituraHighMidi)
                put("rangeSpanSemitones", song.rangeSpanSemitones)
                put("highNoteBurden", song.highNoteBurden)
                put("longNoteBurden", song.longNoteBurden)
                put("leapDifficulty", song.leapDifficulty)
                put("rhythmDifficulty", song.rhythmDifficulty)
                put("overallDifficulty", song.overallDifficulty)
                put("recommendedKeyShiftMin", song.recommendedKeyShiftMin)
                put("recommendedKeyShiftMax", song.recommendedKeyShiftMax)
                if (song.audioUrl != null) put("audioUrl", song.audioUrl)
                put("dataSource", song.dataSource)
                put("credibility", song.credibility.name)
                put("dataVersion", song.dataVersion)
                if (song.importBatchId != null) put("importBatchId", song.importBatchId)
            }
        }
    return json.encodeToString(JsonElement.serializer(), JsonArray(objects))
}
