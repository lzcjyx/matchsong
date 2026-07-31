package matchsong.core.testing.fixtures

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * M4.6-1 合成音频夹具生成（PLAN M4.6，test-fixture-manifest.md §2.1）。
 *
 * 生成 44.1k/16bit/mono WAV 到 [FIXTURE_DIR]（core:testing/src/test/resources/audio-fixtures/），
 * 每条夹具附元数据 JSON（来源/参数/生成时间/预期输出）。
 * 运行：./gradlew :core:testing:testDebugUnitTest --tests "*FixtureGenerator*"
 *
 * 注意：夹具资源目录与 src/test/resources 冲突（测试运行时会打包），
 * 故生成到 src/main/resources/audio-fixtures（main source set 资源，测试可读）。
 */
class FixtureGeneratorTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        /** 夹具根目录（main source set 资源；测试 cwd = 模块目录 core/testing）。 */
        val FIXTURE_DIR = File("src/main/resources/audio-fixtures")
        const val SAMPLE_RATE = 44100
    }

    @Test
    fun `generate synthetic fixtures`() {
        FIXTURE_DIR.mkdirs()
        // 与 test-fixture-manifest §2.1 对齐的合成夹具
        generate("FIX-SINE-130", 2.0) { t -> 0.8 * sin(2 * PI * 130.0 * t) }
        generate("FIX-SINE-220", 2.0) { t -> 0.8 * sin(2 * PI * 220.0 * t) }
        generate("FIX-SINE-440", 2.0) { t -> 0.8 * sin(2 * PI * 440.0 * t) }
        generate("FIX-SINE-880", 2.0) { t -> 0.8 * sin(2 * PI * 880.0 * t) }
        generate("FIX-SINE-1046", 2.0) { t -> 0.8 * sin(2 * PI * 1046.0 * t) }
        generate("FIX-SILENCE", 2.0) { _ -> 0.0 }
        generate("FIX-NOISE-WHITE", 2.0, seed = 42L) { _ ->
            0.0 // NOISE 分支在 generate 内用 rnd 生成
        }
        generate("FIX-CLIPPED-440", 2.0) { t ->
            (1.5 * sin(2 * PI * 440.0 * t)).coerceIn(-1.0, 1.0)
        }
        // 音阶 C3-E3-G3-C4（每音 0.5s）
        generateScale("FIX-SCALE-C3-E3-G3-C4", doubleArrayOf(130.81, 164.81, 196.0, 261.63))
        // 说话近似（150Hz 基频 + 谐波 + AM）
        generate("FIX-TALK-150", 2.0, seed = 7L) { t ->
            val harm = 0.5 * sin(2 * PI * 150.0 * t) + 0.25 * sin(2 * PI * 300.0 * t)
            val mod = 0.5 + 0.5 * cos(2 * PI * 3.0 * t)
            0.6 * harm * mod
        }
        writeManifest()
        println("夹具已生成到 ${FIXTURE_DIR.absolutePath}")
    }

    private fun generate(
        name: String,
        durationSec: Double,
        seed: Long? = null,
        sampleFn: (Double) -> Double,
    ) {
        val n = (SAMPLE_RATE * durationSec).toInt()
        val rnd = seed?.let { java.util.Random(it) }
        val pcm = File(tempDir, "$name.pcm")
        val out = java.io.DataOutputStream(pcm.outputStream())
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val v =
                if (rnd != null && name.contains("NOISE")) {
                    // 白噪声：每样本独立随机（固定种子可复现）
                    (rnd.nextDouble() * 2.0 - 1.0) * 0.4
                } else {
                    sampleFn(t)
                }
            val sample = (v * 32767).toInt().coerceIn(-32768, 32767)
            out.writeByte(sample and 0xFF)
            out.writeByte((sample shr 8) and 0xFF)
        }
        out.close()
        writeWav(pcm, File(FIXTURE_DIR, "$name.wav"))
        writeMetadata(name, durationSec, seed)
        pcm.delete()
    }

    private fun generateScale(
        name: String,
        freqs: DoubleArray,
    ) {
        val perNote = (SAMPLE_RATE * 0.5).toInt()
        val pcm = File(tempDir, "$name.pcm")
        val out = java.io.DataOutputStream(pcm.outputStream())
        for (f in freqs) {
            for (i in 0 until perNote) {
                val v = 0.8 * sin(2 * PI * f * i / SAMPLE_RATE)
                val sample = (v * 32767).toInt().coerceIn(-32768, 32767)
                out.writeByte(sample and 0xFF)
                out.writeByte((sample shr 8) and 0xFF)
            }
        }
        out.close()
        writeWav(pcm, File(FIXTURE_DIR, "$name.wav"))
        writeMetadata(name, freqs.size * 0.5, null)
        pcm.delete()
    }

    private fun writeWav(
        pcm: File,
        wav: File,
    ) {
        val dataSize = pcm.length().toInt()
        wav.outputStream().use { os ->
            // RIFF header（44 字节，与 WavFileWriter/WavTestFileFactory 同格式）
            os.write("RIFF".toByteArray())
            os.write(intLE(36 + dataSize))
            os.write("WAVE".toByteArray())
            os.write("fmt ".toByteArray())
            os.write(intLE(16)) // fmt chunk size
            os.write(shortLE(1)) // PCM
            os.write(shortLE(1)) // mono
            os.write(intLE(SAMPLE_RATE))
            os.write(intLE(SAMPLE_RATE * 2)) // byte rate
            os.write(shortLE(2)) // block align
            os.write(shortLE(16)) // bits
            os.write("data".toByteArray())
            os.write(intLE(dataSize))
            os.write(pcm.readBytes())
        }
    }

    private fun writeMetadata(
        name: String,
        durationSec: Double,
        seed: Long?,
    ) {
        val meta = File(FIXTURE_DIR, "$name.meta.json")
        meta.writeText(
            "{\n" +
                "  \"id\": \"$name\",\n" +
                "  \"type\": \"synthetic\",\n" +
                "  \"format\": \"44.1k/16bit/mono\",\n" +
                "  \"durationSec\": $durationSec,\n" +
                "  \"seed\": ${seed ?: "null"},\n" +
                "  \"source\": \"FixtureGeneratorTest (M4.6-1)\",\n" +
                "  \"expected\": \"见 test-fixture-manifest.md §2.1\"\n" +
                "}\n",
        )
    }

    private fun writeManifest() {
        val manifest = File(FIXTURE_DIR, "MANIFEST.md")
        manifest.writeText(
            "# 音频夹具清单（M4.6-1 生成）\n\n" +
                "合成夹具由 FixtureGeneratorTest 生成（44.1k/16bit/mono），预期见 docs/testing/test-fixture-manifest.md §2.1。\n" +
                "生成命令：`./gradlew :core:testing:testDebugUnitTest --tests \"*FixtureGenerator*\"`\n",
        )
    }

    private fun intLE(v: Int): ByteArray =
        byteArrayOf(
            (v and 0xFF).toByte(),
            ((v shr 8) and 0xFF).toByte(),
            ((v shr 16) and 0xFF).toByte(),
            ((v shr 24) and 0xFF).toByte(),
        )

    private fun shortLE(v: Int): ByteArray = byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())
}
