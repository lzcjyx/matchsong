package matchsong.core.model.song

/**
 * 受控风格词表（M6.1-1、data-model §2.8「受控风格词表（M6 定义）」）。
 *
 * MVP 固定 12 类中文风格标签；导入数据必须落在 [ALL] 内
 * （M6.1-2 song-schema.json 的 genre enum 与此词表保持一致，M6.3 MVP 数据集同源）。
 * 词表变更时需同步 song-schema.json 的 genre 枚举。
 */
object Genre {
    /** 全部受控风格标签。 */
    val ALL: List<String> =
        listOf(
            "流行", "摇滚", "民谣", "R&B", "嘻哈", "电子", "爵士", "古典", "乡村", "金属", "蓝调", "其他",
        )

    /** 判断 [genre] 是否为受控词表内的合法风格。 */
    fun isValid(genre: String): Boolean = genre in ALL
}
