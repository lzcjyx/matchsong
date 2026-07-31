package matchsong.core.audio.algorithm

import kotlin.math.ln
import kotlin.math.pow

/**
 * M5.2-2 频率 ↔ MIDI ↔ 音名转换（FR-SONG-5：MIDI 内部标准，data-model §1.1）。
 *
 * 约定：midi = 69 + 12*log2(f/440)，60 = C4；展示取整转音名（默认升号）。
 */
object PitchNotation {
    /** 频率 → MIDI 音符（Double：整数半音 + 小数音分）。 */
    fun freqToMidi(freqHz: Double): Double = 69.0 + 12.0 * ln(freqHz / 440.0) / ln(2.0)

    /** MIDI 音符 → 频率（Hz）。 */
    fun midiToFreq(midi: Double): Double = 440.0 * 2.0.pow((midi - 69.0) / 12.0)

    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** MIDI 音符 → 音名（如 69.0 → "A4"）；默认升号策略。 */
    fun midiToNoteName(midi: Double): String {
        val rounded = Math.round(midi).toInt()
        val name = NAMES[((rounded % 12) + 12) % 12]
        val octave = rounded / 12 - 1
        return "$name$octave"
    }

    /** 频率 → 音名（如 440.0 → "A4"）。 */
    fun freqToNoteName(freqHz: Double): String = midiToNoteName(freqToMidi(freqHz))

    /** 半音差（音分）：两频率差（cents）。 */
    fun freqDiffCents(
        f1: Double,
        f2: Double,
    ): Double = 1200.0 * ln(f1 / f2) / ln(2.0)

    /** 边界参考（ADR-003）：C2=65.41Hz(MIDI 36)、C6=1046.5Hz(MIDI 84)。 */
    const val C2_FREQ = 65.406
    const val C6_FREQ = 1046.502
    const val C2_MIDI = 36.0
    const val C6_MIDI = 84.0
}
