package matchsong.core.audio.algorithm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * M5.2-2 频率 ↔ MIDI ↔ 音名转换测试（FR-SONG-5，data-model §1.1）。
 */
class PitchNotationTest {
    @Test
    fun `A4 440 maps to midi 69`() {
        assertEquals(69.0, PitchNotation.freqToMidi(440.0), 0.001)
        assertEquals("A4", PitchNotation.freqToNoteName(440.0))
    }

    @Test
    fun `C4 261_63 maps to midi 60`() {
        assertEquals(60.0, PitchNotation.freqToMidi(261.63), 0.01)
        assertEquals("C4", PitchNotation.freqToNoteName(261.63))
    }

    @Test
    fun `boundaries C2 and C6`() {
        assertEquals(36.0, PitchNotation.freqToMidi(PitchNotation.C2_FREQ), 0.01)
        assertEquals(84.0, PitchNotation.freqToMidi(PitchNotation.C6_FREQ), 0.01)
        assertEquals("C2", PitchNotation.freqToNoteName(PitchNotation.C2_FREQ))
        assertEquals("C6", PitchNotation.freqToNoteName(PitchNotation.C6_FREQ))
    }

    @Test
    fun `midi to freq roundtrip`() {
        for (midi in intArrayOf(36, 48, 60, 69, 84)) {
            val freq = PitchNotation.midiToFreq(midi.toDouble())
            assertEquals(midi.toDouble(), PitchNotation.freqToMidi(freq), 0.001)
        }
    }

    @Test
    fun `freq diff cents`() {
        // 440 → 880 = 1200 音分（一个八度）
        assertEquals(1200.0, PitchNotation.freqDiffCents(880.0, 440.0), 0.01)
        // 半音 ≈ 100 音分
        assertEquals(100.0, PitchNotation.freqDiffCents(466.16, 440.0), 1.0)
    }
}
