package matchsong.data.songs

/**
 * 导入报告（M6.2-3）。
 *
 * @param total 文件条目总数
 * @param successCount 成功条目数（解析 + 校验均通过）
 * @param failureCount 失败条目数（= total − successCount）
 * @param entryErrors 失败条目明细（解析错误 index 为文件位置、校验错误 index 为歌曲列表
 *   位置；reason 内含 songId/title 便于定位）
 */
data class ImportReport(
    val total: Int,
    val successCount: Int,
    val failureCount: Int,
    val entryErrors: List<EntryError>,
)
