package matchsong.core.model.song

/**
 * 歌曲数据可信度（FR-SONG-1、data-model §2.8）。
 *
 * 由导入工具按来源质量分级（M6.3 来源登记表），供推荐引擎在数据质量降级时使用
 * （LOW 可信度的歌曲不参与正式推荐或需降权）。
 */
enum class Credibility {
    /** 高可信：来源可靠且经过验证（如官方谱面、人工复核）。 */
    HIGH,

    /** 中等可信：来源基本可靠，但未经完整复核。 */
    MEDIUM,

    /** 低可信：来源不确定或仅粗略标注，使用前需降级处理。 */
    LOW,
}
