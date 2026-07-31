package matchsong.core.testing.fixtures

import matchsong.core.testing.WavReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * M4.6-1 夹具清单校验（PLAN M4.6 验收：测试夹具拥有来源和预期）。
 *
 * 校验：所有 .wav 存在对应 .meta.json；WAV 可被 WavReader 回读且格式正确（44.1k/16bit/mono）。
 */
class FixtureManifestTest {
    private val fixtureDir = File("src/main/resources/audio-fixtures")

    @Test
    fun `all wav fixtures have metadata and valid format`() {
        val wavs = fixtureDir.listFiles { f -> f.extension == "wav" }.orEmpty()
        assertTrue(wavs.isNotEmpty(), "夹具目录不应为空：${fixtureDir.absolutePath}")
        for (wav in wavs) {
            val meta = File(fixtureDir, wav.nameWithoutExtension + ".meta.json")
            assertTrue(meta.exists(), "${wav.name} 缺少元数据 ${meta.name}")

            val data = WavReader().read(wav)
            assertEquals(44_100, data.sampleRateHz, "${wav.name} 采样率应为 44.1k")
            assertEquals(1, data.channels, "${wav.name} 应为单声道")
            assertEquals(16, data.bitsPerSample, "${wav.name} 应为 16bit")
            assertTrue(data.frameCount > 0, "${wav.name} 不应为空")
        }
    }

    @Test
    fun `manifest lists all fixtures`() {
        val manifest = File(fixtureDir, "MANIFEST.md")
        assertTrue(manifest.exists(), "缺少 MANIFEST.md")
        val wavCount = fixtureDir.listFiles { f -> f.extension == "wav" }.orEmpty().size
        assertTrue(wavCount >= 10, "合成夹具应 ≥10 个，实际 $wavCount")
    }
}
