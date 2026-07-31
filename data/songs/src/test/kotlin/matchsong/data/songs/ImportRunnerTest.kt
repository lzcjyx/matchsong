package matchsong.data.songs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * M6.2-3 ImportRunner 测试：错误数据集 → 报告计数精确；CLI 渲染可读；main 入口存在。
 */
class ImportRunnerTest {
    @TempDir
    lateinit var tempDir: File

    private fun resourceAsTempFile(resourcePath: String): File {
        val content =
            javaClass.getResource(resourcePath)?.readText(Charsets.UTF_8)
                ?: error("测试资源缺失：$resourcePath")
        val target = File(tempDir, File(resourcePath).name)
        target.writeText(content, Charsets.UTF_8)
        return target
    }

    @Test
    fun `error dataset produces exact report counts`() {
        // invalid-songs.json：4 条 = 1 成功 + 1 解析失败（缺 songId）+ 1 校验失败（最低>最高）+ 1 校验失败（songId 重复）
        val file = resourceAsTempFile("/invalid-songs.json")
        val report = ImportRunner.runImport(file)
        assertEquals(4, report.total)
        assertEquals(1, report.successCount)
        assertEquals(3, report.failureCount)
        assertEquals(3, report.entryErrors.size)

        val parseErrors = report.entryErrors.filter { it.reason.contains("解析失败") }
        assertEquals(1, parseErrors.size)
        assertEquals(1, parseErrors[0].index, "缺 songId 的条目在数组下标 1")

        val validationErrors =
            report.entryErrors.filter {
                it.reason.contains("songId 重复") || it.reason.contains("lowestMidi")
            }
        assertEquals(2, validationErrors.size)
    }

    @Test
    fun `valid json dataset reports all success`() {
        val file = resourceAsTempFile("/sample-songs.json")
        val report = ImportRunner.runImport(file)
        assertEquals(3, report.total)
        assertEquals(3, report.successCount)
        assertEquals(0, report.failureCount)
        assertTrue(report.entryErrors.isEmpty())
    }

    @Test
    fun `valid csv dataset reports all success`() {
        val file = resourceAsTempFile("/sample-songs.csv")
        val report = ImportRunner.runImport(file)
        assertEquals(3, report.total)
        assertEquals(3, report.successCount)
        assertEquals(0, report.failureCount)
    }

    @Test
    fun `report rendering contains stats and details`() {
        val file = resourceAsTempFile("/invalid-songs.json")
        val text = ImportRunner.renderReport(ImportRunner.runImport(file))
        assertTrue(text.contains("条目总数：4"), text)
        assertTrue(text.contains("成功：1"), text)
        assertTrue(text.contains("失败：3"), text)
        assertTrue(text.contains("失败明细"), text)
        assertTrue(text.contains("条目 #1"), text)
    }

    @Test
    fun `main entry exists with jvm static modifier`() {
        val method = ImportRunner::class.java.getMethod("main", Array<String>::class.java)
        org.junit.jupiter.api.Assertions.assertNotNull(method)
        assertTrue(java.lang.reflect.Modifier.isStatic(method.modifiers))
    }
}
