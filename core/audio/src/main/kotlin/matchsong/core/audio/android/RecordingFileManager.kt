package matchsong.core.audio.android

import matchsong.core.audio.algorithm.WavFileWriter
import matchsong.core.common.error.AppError.StorageError
import matchsong.core.common.result.OperationResult
import matchsong.core.common.result.OperationResult.Failure
import matchsong.core.common.result.OperationResult.Success
import matchsong.domain.port.RecordingFileCleaner
import java.io.File
import java.io.IOException

/**
 * M3.5-1 录音临时文件管理器（ARCHITECTURE.md §7.3，FR-REC-7/FR-REC-8，P8）。
 *
 * 目录：`cacheDir/recordings/`（缓存目录，系统可清理）：
 * - `{sessionId}.pcm`：录音中 AudioRecord 原始 PCM 流（44.1kHz/16bit/mono）；
 * - `{sessionId}.wav`：停止后补 WAV header（[finalizeWav]，FR-REC-7），供质量检测与分析。
 *
 * 生命周期（§7.3）：创建 .pcm → 停止写 .wav → 质量门禁+分析消费 .wav →
 * 分析完成/取消/失败 `finally` 中删除 .pcm 与 .wav → 下次启动 [cleanStale] 清理残留。
 *
 * 实现说明：纯 java.io（无 android import），JVM 单测可直测；
 * 目录与 [freeSpaceProvider]（空间检查）均构造器注入，便于测试
 * StorageError.NoSpace 分支。app DI 传入 `context.cacheDir/recordings`（AppModule）。
 *
 * 错误语义（P9）：IO 失败映射为 [StorageError.Io]，空间不足映射为 [StorageError.NoSpace]，
 * 通过 [OperationResult] 返回，禁止空 catch。
 */
class RecordingFileManager(
    private val recordingsDir: File,
    private val freeSpaceProvider: (File) -> Long = { it.usableSpace },
) : RecordingFileCleaner {
    /**
     * 创建会话 PCM 文件（`{sessionId}.pcm`）。
     *
     * 录音前检查可用空间（FR-REC-2/SPEC §6：30s ≈ 2.65MB + 余量）；
     * 空间不足返回 [StorageError.NoSpace]。文件已存在视为幂等成功（重试场景）。
     *
     * @param sessionId 会话 ID（UUID 字符串，data-model.md §2.1）。
     * @return 成功携带 .pcm 文件；失败为 [StorageError.NoSpace] / [StorageError.Io]。
     */
    fun createSessionFiles(sessionId: String): OperationResult<File> {
        val dir = recordingsDir
        return try {
            when {
                !dir.isDirectory && !dir.mkdirs() -> {
                    Failure(StorageError.Io(IOException("无法创建录音目录：${dir.name}")))
                }
                freeSpaceProvider(dir) < MIN_FREE_BYTES_FOR_SESSION -> {
                    Failure(StorageError.NoSpace)
                }
                else -> {
                    val pcm = pcmFile(sessionId)
                    pcm.createNewFile() // 已存在时返回 false，视为幂等成功
                    Success(pcm)
                }
            }
        } catch (e: IOException) {
            Failure(StorageError.Io(e))
        }
    }

    /**
     * 将 `{sessionId}.pcm` 封装为 `{sessionId}.wav`（FR-REC-7）。
     *
     * 格式默认 ADR-002 / data-model.md §2.2：44.1kHz / 16bit / mono；
     * PCM 文件在分析完成前保留（§7.3 生命周期，由 [deleteSessionFiles] 统一删除）。
     *
     * @return 成功携带 .wav 文件；失败为 [StorageError.Io]（如 PCM 缺失）。
     */
    fun finalizeWav(
        sessionId: String,
        sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ,
        channels: Int = DEFAULT_CHANNELS,
        bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE,
    ): OperationResult<File> {
        return try {
            Success(
                WavFileWriter.writePcmToWav(
                    pcmFile = pcmFile(sessionId),
                    wavFile = wavFile(sessionId),
                    sampleRateHz = sampleRateHz,
                    channels = channels,
                    bitsPerSample = bitsPerSample,
                ),
            )
        } catch (e: IOException) {
            Failure(StorageError.Io(e))
        }
    }

    /**
     * 删除会话的 .pcm 与 .wav（取消/失败/分析完成后的 finally 清理，§7.3，P8）。
     * 文件不存在视为幂等成功；存在但删除失败返回 [StorageError.Io]。
     */
    fun deleteSessionFiles(sessionId: String): OperationResult<Unit> {
        val failed =
            listOf(pcmFile(sessionId), wavFile(sessionId)).filter { file ->
                file.exists() && !file.delete()
            }
        return if (failed.isEmpty()) {
            Success(Unit)
        } else {
            Failure(StorageError.Io(IOException("删除录音文件失败：${failed.map { it.name }}")))
        }
    }

    /**
     * M3.5-2 清理过期残留（Port 实现，FR-REC-8）：删除修改时间早于
     * `now - olderThanMs` 且不属于 [activeSessionIds] 的 .pcm/.wav；
     * 目录不存在时返回 0。单文件删除失败跳过（尽力而为，见 Port 契约）。
     */
    override suspend fun cleanStale(
        olderThanMs: Long,
        activeSessionIds: Set<String>,
    ): Int {
        val dir = recordingsDir
        if (!dir.isDirectory) return 0
        val cutoffMs = System.currentTimeMillis() - olderThanMs
        var deleted = 0
        dir.listFiles { file ->
            file.isFile && (file.name.endsWith(PCM_SUFFIX) || file.name.endsWith(WAV_SUFFIX))
        }?.forEach { file ->
            val sessionId = file.name.removeSuffix(PCM_SUFFIX).removeSuffix(WAV_SUFFIX)
            if (sessionId !in activeSessionIds && file.lastModified() < cutoffMs && file.delete()) {
                deleted++
            }
        }
        return deleted
    }

    /**
     * M9.3 清空录音缓存目录全部 .pcm/.wav（删除全部数据，FR-PRIV-5/ACC-15）。
     * 尽力而为：单文件删除失败跳过；目录不存在时返回 0。
     */
    override suspend fun clearAll(): Int {
        val dir = recordingsDir
        if (!dir.isDirectory) return 0
        var deleted = 0
        dir.listFiles { file ->
            file.isFile && (file.name.endsWith(PCM_SUFFIX) || file.name.endsWith(WAV_SUFFIX))
        }?.forEach { file ->
            if (file.delete()) {
                deleted++
            }
        }
        return deleted
    }

    private fun pcmFile(sessionId: String): File = File(recordingsDir, "$sessionId$PCM_SUFFIX")

    private fun wavFile(sessionId: String): File = File(recordingsDir, "$sessionId$WAV_SUFFIX")

    companion object {
        /** 录音缓存子目录名（cacheDir 下，§7.3）。 */
        const val RECORDINGS_DIR_NAME: String = "recordings"

        /** PCM 临时文件后缀。 */
        const val PCM_SUFFIX: String = ".pcm"

        /** WAV 输出文件后缀。 */
        const val WAV_SUFFIX: String = ".wav"

        /** 默认录音格式（ADR-002 / data-model.md §2.2）。 */
        const val DEFAULT_SAMPLE_RATE_HZ: Int = 44_100

        const val DEFAULT_CHANNELS: Int = 1

        const val DEFAULT_BITS_PER_SAMPLE: Int = 16

        /**
         * 单次录音所需最低可用空间：30s × 44.1kHz × 16bit × mono ≈ 2.65MB
         * （SPEC §6），预留约 30% 余量。
         */
        const val MIN_FREE_BYTES_FOR_SESSION: Long = 3_500_000L
    }
}
