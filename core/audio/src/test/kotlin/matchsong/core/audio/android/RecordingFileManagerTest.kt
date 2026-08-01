package matchsong.core.audio.android

import kotlinx.coroutines.test.runTest
import matchsong.core.audio.algorithm.WavFileReader
import matchsong.core.common.error.AppError.StorageError
import matchsong.core.common.result.OperationResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * M3.5-1 录音文件管理器测试（FR-REC-7/FR-REC-8，ARCHITECTURE.md §7.3 生命周期）。
 *
 * 纯 java.io 实现，JVM 直测：创建/封装/删除/清理 + 空间不足分支（注入
 * [freeSpaceProvider] 模拟，StorageError.NoSpace）。
 */
class RecordingFileManagerTest {
    @TempDir
    lateinit var tempDir: File

    private fun manager(freeSpace: (File) -> Long = { Long.MAX_VALUE }): RecordingFileManager =
        RecordingFileManager(File(tempDir, "recordings"), freeSpace)

    private fun pcmBytes(shorts: ShortArray): ByteArray =
        ByteBuffer.allocate(shorts.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            shorts.forEach { putShort(it) }
        }.array()

    // ---------- createSessionFiles ----------

    @Test
    fun `createSessionFiles 创建 {sessionId}_pcm 并返回该文件`() {
        val result = manager().createSessionFiles("s1")

        assertTrue(result is OperationResult.Success)
        val pcm = (result as OperationResult.Success).data
        assertEquals("s1.pcm", pcm.name)
        assertTrue(pcm.isFile)
        assertTrue(pcm.parentFile.name == "recordings")
    }

    @Test
    fun `空间不足时返回 NoSpace 且不创建文件`() {
        val m = manager(freeSpace = { 0L })

        val result = m.createSessionFiles("s1")

        assertTrue(result is OperationResult.Failure)
        assertTrue((result as OperationResult.Failure).error is StorageError.NoSpace)
        assertFalse(File(tempDir, "recordings/s1.pcm").exists())
    }

    @Test
    fun `空间刚好满足阈值时成功`() {
        val m = manager(freeSpace = { RecordingFileManager.MIN_FREE_BYTES_FOR_SESSION })

        assertTrue(m.createSessionFiles("s1") is OperationResult.Success)
    }

    @Test
    fun `createSessionFiles 幂等：文件已存在时仍成功`() {
        val m = manager()
        assertTrue(m.createSessionFiles("s1") is OperationResult.Success)

        assertTrue(m.createSessionFiles("s1") is OperationResult.Success)
    }

    // ---------- finalizeWav ----------

    @Test
    fun `finalizeWav 生成标准 WAV 且内容与 PCM 一致`() {
        val m = manager()
        val shorts = shortArrayOf(1000, -2000, 3000, -4000, 5000, -6000)
        m.createSessionFiles("s1")
        File(tempDir, "recordings/s1.pcm").writeBytes(pcmBytes(shorts))

        val result = m.finalizeWav("s1")

        assertTrue(result is OperationResult.Success)
        val wav = (result as OperationResult.Success).data
        assertEquals("s1.wav", wav.name)
        assertTrue(wav.isFile)
        // PCM 在分析完成前保留（§7.3 生命周期）
        assertTrue(File(tempDir, "recordings/s1.pcm").isFile)

        val data = WavFileReader().read(wav)
        assertEquals(44_100, data.sampleRateHz)
        assertEquals(1, data.channels)
        assertEquals(16, data.bitsPerSample)
        assertEquals(6, data.frameCount)
        org.junit.jupiter.api.Assertions.assertArrayEquals(shorts, data.samples)
    }

    @Test
    fun `finalizeWav 缺少 PCM 时返回 Io 错误`() {
        val result = manager().finalizeWav("missing")

        assertTrue(result is OperationResult.Failure)
        assertTrue((result as OperationResult.Failure).error is StorageError.Io)
    }

    // ---------- deleteSessionFiles ----------

    @Test
    fun `deleteSessionFiles 删除 pcm 与 wav`() {
        val m = manager()
        m.createSessionFiles("s1")
        m.finalizeWav("s1")

        assertTrue(m.deleteSessionFiles("s1") is OperationResult.Success)
        assertFalse(File(tempDir, "recordings/s1.pcm").exists())
        assertFalse(File(tempDir, "recordings/s1.wav").exists())
    }

    @Test
    fun `deleteSessionFiles 无文件时幂等成功`() {
        assertTrue(manager().deleteSessionFiles("ghost") is OperationResult.Success)
    }

    // ---------- cleanStale（M3.5-2） ----------

    @Test
    fun `cleanStale 删除过期残留并保留活跃会话与新鲜文件`() =
        runTest {
            val m = manager()
            val now = System.currentTimeMillis()
            val old = now - 2 * 60 * 60 * 1000L // 2h 前
            m.createSessionFiles("a")
            m.createSessionFiles("b")
            m.createSessionFiles("c")
            m.createSessionFiles("d")
            File(tempDir, "recordings/a.pcm").setLastModified(old)
            File(tempDir, "recordings/b.pcm").setLastModified(old)
            // c.pcm 保持创建时间（新鲜，最近修改），d.pcm 设为旧
            File(tempDir, "recordings/d.pcm").setLastModified(old)

            // 过期阈值 1h：a/b/d 过期，c 新鲜；d 为活跃会话（保留）
            val deleted = m.cleanStale(olderThanMs = 60 * 60 * 1000L, activeSessionIds = setOf("d"))

            assertEquals(2, deleted) // 仅 a、b 被删
            assertFalse(File(tempDir, "recordings/a.pcm").exists())
            assertFalse(File(tempDir, "recordings/b.pcm").exists())
            assertTrue(File(tempDir, "recordings/c.pcm").exists())
            assertTrue(File(tempDir, "recordings/d.pcm").exists())
        }

    @Test
    fun `cleanStale 删除过期 wav 残留`() =
        runTest {
            val m = manager()
            val old = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
            m.createSessionFiles("x")
            File(tempDir, "recordings/x.wav").writeBytes(byteArrayOf(1, 2, 3))
            File(tempDir, "recordings/x.pcm").setLastModified(old)
            File(tempDir, "recordings/x.wav").setLastModified(old)

            assertEquals(2, m.cleanStale(olderThanMs = 60 * 60 * 1000L, activeSessionIds = emptySet()))
            assertTrue(File(tempDir, "recordings").listFiles().orEmpty().isEmpty())
        }

    @Test
    fun `cleanStale 空目录或不存在目录返回 0`() =
        runTest {
            val m = manager()
            assertEquals(0, m.cleanStale(olderThanMs = 1000L, activeSessionIds = emptySet()))
            assertEquals(
                0,
                RecordingFileManager(File(tempDir, "not-created"), { Long.MAX_VALUE })
                    .cleanStale(olderThanMs = 1000L, activeSessionIds = emptySet()),
            )
        }

    @Test
    fun `cleanStale 忽略非录音文件`() =
        runTest {
            val m = manager()
            val old = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
            val junk =
                File(tempDir, "recordings/junk.txt").apply {
                    parentFile.mkdirs()
                    writeText("x")
                }
            junk.setLastModified(old)

            assertEquals(0, m.cleanStale(olderThanMs = 60 * 60 * 1000L, activeSessionIds = emptySet()))
            assertTrue(junk.exists())
        }

    // ---------- clearAll（M9.3，FR-PRIV-5/ACC-15） ----------

    @Test
    fun `clearAll 清空全部 pcm 与 wav`() =
        runTest {
            val m = manager()
            m.createSessionFiles("a")
            m.createSessionFiles("b")
            m.finalizeWav("a")
            File(tempDir, "recordings/keep.txt").writeText("junk")

            assertEquals(3, m.clearAll())
            val remaining = File(tempDir, "recordings").listFiles().orEmpty().map { it.name }.sorted()
            assertEquals(listOf("keep.txt"), remaining, "非录音文件应保留")
        }

    @Test
    fun `clearAll 目录不存在时返回 0`() =
        runTest {
            val m = RecordingFileManager(File(tempDir, "not-created"), { Long.MAX_VALUE })
            assertEquals(0, m.clearAll())
        }
}
