package matchsong.data.songs

import java.io.File
import kotlin.system.exitProcess

/**
 * 歌曲数据导入工具入口（M6.2-3，独立于 App Runtime）。
 *
 * 用法：`ImportRunner <数据文件路径>`（.csv 按 CSV 解析，其余按 JSON 数组解析）。
 * 流程：读取 → 解析（M6.2-1）→ 校验（M6.2-2）→ 输出文本报告（成功 N / 失败 M，
 * 失败条目与原因），供人工与 M6.3-2 数据发布流程消费。
 *
 * 仅依赖 data:songs + core:model，不打包任何 App 运行时组件
 * （PLAN M6.2「导入工具和 App Runtime 解耦」）。
 *
 * 退出码：0 = 全部成功（含空文件）；1 = 存在失败条目；2 = 用法或读取错误。
 */
object ImportRunner {
    /** 用法提示。 */
    const val USAGE: String = "用法：ImportRunner <数据文件路径>（.csv 按 CSV 解析，其余按 JSON 数组解析）"

    /**
     * JVM 入口（独立运行，不依赖 App 运行时）。
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val filePath = args.firstOrNull()
        if (filePath == null) {
            println(USAGE)
            exitProcess(2)
        }
        val file = File(filePath)
        if (!file.isFile) {
            println("错误：文件不存在或不是普通文件：$filePath")
            exitProcess(2)
        }
        val report = runImport(file)
        print(renderReport(report))
        exitProcess(if (report.failureCount == 0) 0 else 1)
    }

    /**
     * 执行一次导入：读取 [file] → 解析 → 校验 → 汇总报告。
     * 供 CLI 入口与 M6.3-2 数据发布流程复用；纯函数可测。
     */
    fun runImport(file: File): ImportReport {
        val text = file.readText(Charsets.UTF_8)
        val parsed =
            if (file.name.endsWith(".csv", ignoreCase = true)) {
                CsvSongParser.parse(text)
            } else {
                SongDataParser.parse(text)
            }
        val validation = SongImportValidator.validate(parsed.songs)
        val entryErrors = parsed.errors + validation.entryErrors
        return ImportReport(
            total = parsed.totalEntryCount,
            successCount = parsed.totalEntryCount - entryErrors.size,
            failureCount = entryErrors.size,
            entryErrors = entryErrors,
        )
    }

    /** 渲染人类可读报告（CLI 输出与测试共用）。 */
    fun renderReport(report: ImportReport): String {
        val builder = StringBuilder()
        builder.appendLine("MatchSong 歌曲数据导入报告")
        builder.appendLine("条目总数：${report.total}")
        builder.appendLine("成功：${report.successCount}")
        builder.appendLine("失败：${report.failureCount}")
        if (report.entryErrors.isNotEmpty()) {
            builder.appendLine("失败明细：")
            report.entryErrors.forEach { error ->
                builder.appendLine("  - 条目 #${error.index}：${error.reason}")
            }
        }
        return builder.toString()
    }
}
