package matchsong.pitch

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 合成信号生成器（Spike 用途，不进生产模块）。
 *
 * 所有信号为单声道 double[]，采样率固定 44100，幅值范围 [-1, 1]。
 */
object Signals {

    const val SAMPLE_RATE = 44100

    /** 正弦波，给定频率 Hz 与时长秒。 */
    fun sine(freqHz: Double, durationSec: Double, amplitude: Double = 0.8): DoubleArray {
        val n = (SAMPLE_RATE * durationSec).toInt()
        return DoubleArray(n) { i ->
            amplitude * sin(2.0 * PI * freqHz * i / SAMPLE_RATE)
        }
    }

    /**
     * 固定音阶：按给定 MIDI 音符序列逐音播放，每个音符 durationSec。
     * 用于模拟“稳定演唱音区”的简化输入。
     */
    fun scale(midiNotes: IntArray, durationSec: Double, amplitude: Double = 0.8): DoubleArray {
        val perNote = (SAMPLE_RATE * durationSec).toInt()
        val out = DoubleArray(perNote * midiNotes.size)
        var idx = 0
        for (midi in midiNotes) {
            val f = midiToFreq(midi)
            for (i in 0 until perNote) {
                out[idx++] = amplitude * sin(2.0 * PI * f * i / SAMPLE_RATE)
            }
        }
        return out
    }

    /** 静音（极低幅值，模拟底噪）。 */
    fun silence(durationSec: Double, amplitude: Double = 1e-5): DoubleArray {
        val n = (SAMPLE_RATE * durationSec).toInt()
        return DoubleArray(n) { amplitude }
    }

    /** 白噪声。 */
    fun whiteNoise(durationSec: Double, amplitude: Double = 0.5): DoubleArray {
        val n = (SAMPLE_RATE * durationSec).toInt()
        val rnd = java.util.Random(42) // 固定种子，可复现
        return DoubleArray(n) { amplitude * (rnd.nextDouble() * 2.0 - 1.0) }
    }

    /** 削波音频：把正弦波硬限幅到 [-clip, clip]，产生谐波失真。 */
    fun clipped(freqHz: Double, durationSec: Double, clip: Double = 0.3): DoubleArray {
        val raw = sine(freqHz, durationSec, amplitude = 1.0)
        return DoubleArray(raw.size) { i ->
            val v = raw[i]
            when {
                v > clip -> clip
                v < -clip -> -clip
                else -> v
            }
        }
    }

    /**
     * 模拟“说话声”：宽带噪声 + 低频基频调制 + 谐波，非稳态。
     * 仅作为“类人声非稳态”的粗略近似，不声称还原真实人声。
     */
    fun talkLike(durationSec: Double, f0: Double = 150.0): DoubleArray {
        val n = (SAMPLE_RATE * durationSec).toInt()
        val rnd = java.util.Random(7)
        return DoubleArray(n) { i ->
            val t = i / SAMPLE_RATE.toDouble()
            val harm = 0.5 * sin(2 * PI * f0 * t) + 0.25 * sin(2 * PI * 2 * f0 * t) + 0.12 * sin(2 * PI * 3 * f0 * t)
            val mod = 0.5 + 0.5 * cos(2 * PI * 3.0 * t) // 3Hz 调制模拟音节起伏
            val noise = 0.15 * (rnd.nextDouble() * 2.0 - 1.0)
            0.6 * harm * mod + noise
        }
    }

    /** MIDI 音符号转频率（A4=440Hz, MIDI=69）。 */
    fun midiToFreq(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
}

/** 频率/音符转换工具。 */
object Notes {
    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun freqToMidi(freqHz: Double): Double = 69 + 12 * log2(freqHz / 440.0)

    fun midiToName(midi: Double): String {
        val m = Math.round(midi).toInt()
        val name = NAMES[((m % 12) + 12) % 12]
        val octave = m / 12 - 1
        return "$name$octave"
    }

    private fun log2(x: Double): Double = kotlin.math.ln(x) / kotlin.math.ln(2.0)
}
