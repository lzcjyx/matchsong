# Vocal / Singing Sample Sources for Pitch-Detection (YIN) Testing

> 用途:为 Android 音域分析 App 的 YIN 基频检测算法准备测试音频(真实人声,男声/女声,优先带已知音高内容的演唱)。
> Purpose: real human-voice test fixtures (male + female, preferably sung with known/expected pitch) for validating a YIN pitch-detection algorithm.
>
> 验证日期 / Verified: **2026-07-31** · 验证方式 / Method: `curl -sI` HEAD requests (HTTP status recorded below); files <10 MB were downloaded to `experiments/vocal-samples/` and validated by header magic bytes; MIR-1K pitch ground truth parsed and statistically summarized.

## 1. 结论速览 / TL;DR

| Rank | Sample | Voice | Why it is recommended |
|---|---|---|---|
| **1** | **MIR-1K `example3.wav`** (via GitHub mirror) | **Male** singing, avg F0 ≈ 184.7 Hz (122–268 Hz) | Clean solo vocal WAV (44.1 kHz mono 16-bit PCM, 27.3 s) **with frame-level pitch ground truth** (`example3REF.txt`, time–Hz, 10 ms hop) — ideal for automated YIN validation with tolerance thresholds |
| **2** | **MIR-1K `example1.wav` / `example2.wav`** (same mirror) | **Female** singing, avg F0 ≈ 284.7 / 291.2 Hz (215–385 / 151–440 Hz) | Same ground-truth setup; ex1 24.5 s, ex2 32.5 s. ex2 contains a wide range (down to 151 Hz) — good stress case |
| **3** | **LibriVox Short Poetry Collection 137** (archive.org) | **Speech**, male + female readers | Public domain, tiny direct MP3s (1.2–1.7 MB), English poetry with clear intonation; supplementary speech-pitch tests |
| (bonus) | **Wikimedia Commons — Caruso "La donna è mobile" (1908)** | Male operatic tenor | Public domain, known Verdi aria melody (B major); historical 1908 recording, OGG (needs conversion) |
| (bonus) | **Wikimedia Commons — Caruso & Melba "O soave fanciulla" (1907)** | Male + female operatic duet | Public domain, known Puccini melody; both voices present in one file |

---

## 2. 候选源总表 / Source Table (all URLs HEAD-verified on 2026-07-31)

| # | Sample name | Source | License | Direct URL | Format | Size / Duration | Expected content (known) | HTTP status |
|---|---|---|---|---|---|---|---|---|
| 1 | MIR-1K example1 (female) | GitHub mirror of MIR-1K (MIREX examples): `vaaiibhav/mirexDatasetmir1k-2004` | MIR-1K: free for research (MIRLab); mirror has **no license file** — attribute MIRLab & mirror | `https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example1.wav` | WAV 44.1 kHz mono 16-bit | 2,160,106 B · 24.5 s | Solo singing, F0 avg 284.7 Hz (214–385 Hz); ground truth `example1REF.txt` (time-TAB-Hz, 10 ms hop) | **200** |
| 2 | MIR-1K example2 (female) | same | same | `https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example2.wav` | WAV 44.1 kHz mono 16-bit | 2,868,240 B · 32.5 s | Solo singing, F0 avg 291.2 Hz (151–440 Hz); REF ground truth | **200** |
| 3 | MIR-1K example3 (male) | same | same | `https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example3.wav` | WAV 44.1 kHz mono 16-bit | 2,410,472 B · 27.3 s | Solo singing, F0 avg 184.7 Hz (122–268 Hz); REF ground truth | **200** |
| 4 | MIR-1K REF labels (×3) | same | same | `.../example1REF.txt`, `.../example2REF.txt`, `.../example3REF.txt` | TXT (time `\t` Hz) | 26–35 KB | Per-frame pitch reference for YIN accuracy evaluation | **200** |
| 5 | Caruso — "La donna è mobile" (Rigoletto, Verdi), ~1908 | Wikimedia Commons `File:La Donna E Mobile Rigoletto.ogg` | **Public domain** (PD-old; CC-PD-Mark) | `https://upload.wikimedia.org/wikipedia/commons/5/5a/La_Donna_E_Mobile_Rigoletto.ogg` | OGG Vorbis (needs →WAV) | 1,541,593 B · 2:10 | Male tenor singing famous B-major aria; known melody/notes | **200** |
| 6 | Caruso & Melba — "O soave fanciulla" (La bohème, Puccini), 1907 | Wikimedia Commons `File:Enrico Caruso - Nellie Melba - La Boheme - O soave fanciulla.ogg` | **Public domain** (PD US / PD-Australia; CC-PD-Mark) | `https://upload.wikimedia.org/wikipedia/commons/9/9d/Enrico_Caruso_-_Nellie_Melba_-_La_Boheme_-_O_soave_fanciulla.ogg` | OGG Vorbis | 2,592,911 B · 3:21 | Tenor + soprano duet; known Puccini melody | **200** |
| 7 | USAF Singing Sergeants — "Core 'ngrato" (1995) | Wikimedia Commons `File:Core 'ngrato.ogg` | **Public domain** (US Air Force work; PD-US; CC-PD-Mark) | `https://upload.wikimedia.org/wikipedia/commons/1/15/Core_%27ngrato.ogg` | OGG Vorbis | 4,135,445 B · 3:36 | Male choir singing Neapolitan song | **200** |
| 8 | Caruso — "Addio a Napoli" (1913) | Wikimedia Commons `File:Addio a Napoli.ogg` | **Public domain** (PD-old; CC-PD-Mark) | `https://upload.wikimedia.org/wikipedia/commons/e/eb/Addio_a_Napoli.ogg` | OGG Vorbis | 2,790,111 B · 3:19 | Male tenor singing Cottrau song (1869) | **200** (first probe 429 rate-limit — transient) |
| 9 | LibriVox — "The Lake Isle of Innisfree" (W. B. Yeats), read by Winston Tharp | LibriVox Short Poetry Collection 137, archive.org item `spc137_1411_librivox` | **Public domain** (CC Public Domain Mark 1.0) | `https://archive.org/download/spc137_1411_librivox/spc137_lakeisleinnisfree_wt_128kb.mp3` | MP3 128 kb/s | 1,265,669 B · 1:18 | Male-read English poetry (speech) | **200** |
| 10 | LibriVox — "Elaine" (E. St. Vincent Millay), read by Shakira Searle | same item | **Public domain** (PDM 1.0) | `https://archive.org/download/spc137_1411_librivox/spc137_elaine_ss_128kb.mp3` | MP3 128 kb/s | 1,730,472 B · 1:47 | Female-read English poetry (speech) | **200** |

### Candidates checked and rejected / noted (status recorded)

| Source | URL checked | HTTP | Verdict |
|---|---|---|---|
| MIRLab official `MIR-1K.rar` | `http://mirlab.org/dataset/public/MIR-1K.rar` | **404** | Dead — use GitHub mirror instead |
| MIRLab official `MIR-1K_for_MIREX.rar` | `http://mirlab.org/dataset/public/MIR-1K_for_MIREX.rar` | **404** | Dead |
| Zenodo "MIR-1K dataset" record 3532216 (and ALL-Pub-SVD-In-One 2641106/3480085) | `https://zenodo.org/records/3532216` | **200** | Metadata-only, `access_right: restricted`, `files: []` — **no download** |
| LibriSpeech dev-clean (OpenSLR #12) | `https://www.openslr.org/resources/12/dev-clean.tar.gz` | **200** | ~337 MB tarball; no per-file URL → impractical; use archive.org LibriVox MP3s instead (LibriSpeech is derived from LibriVox audio, same PD source) |
| Mozilla Common Voice | `https://commonvoice.mozilla.org/en/datasets` | **200** | Corpus tarballs only (GB-scale); no stable per-clip public URLs; unofficial HF mirrors (`fsicoli/common_voice_22_0`) exist but licensing/attribution is murkier — LibriVox covers the speech need |
| freesound.org | `https://freesound.org/` | **200** | Download requires API token; per-clip URLs not stable for curl → not suitable for a scripted fixture pipeline |
| Choral Public Domain Library (CPDL) | `https://www.cpdl.org/wiki/index.php/Main_Page` | **403** | Blocks curl (bot protection); per-file URLs unreliable → not recommended |
| GTZAN / RWC / MedleyDB / iKala / VocalSet / M4Singer / DAMP | — | — | Music or non-commercial-only licenses (CC-BY-NC etc.) or registration-gated → excluded per requirements |

---

## 3. 推荐 Top 3 / Recommendations

### 🥇 1. MIR-1K `example3.wav` — male singing with pitch ground truth
- Direct: `https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example3.wav` + `example3REF.txt`
- Why: only source with **exact per-frame pitch reference** (Hz, 10 ms grid) — lets tests assert YIN error ≤ tolerance (e.g., ±1 semitone) instead of eyeballing. Male range 122–268 Hz matches the app's male vocal-range use case.
- License: MIR-1K is free for research use (per MIRLab's own page); the GitHub mirror carries no license file — record both in the manifest.

### 🥈 2. MIR-1K `example1.wav` (+ `example2.wav`) — female singing with pitch ground truth
- Direct: `https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example1.wav` (+ REF), same for example2.
- Why: female F0 215–440 Hz (ex2 dips to 151 Hz and reaches 440 Hz — good octave/vibrato stress test), same ground-truth evaluation pipeline as #1.

### 🥉 3. LibriVox poetry — speech alternative (male + female)
- Direct: `https://archive.org/download/spc137_1411_librivox/spc137_lakeisleinnisfree_wt_128kb.mp3` (male, 1:18) and `.../spc137_elaine_ss_128kb.mp3` (female, 1:47)
- Why: unambiguous public domain, tiny, direct, no login; poetry reading gives sustained, expressive intonation closer to singing than flat reading.

---

## 4. 最终下载命令 / Exact curl commands

```bash
# ---- MIR-1K (singing, with pitch ground truth) ----
# female example 1 (24.5 s) + ground truth
curl -sL -o experiments/vocal-samples/mir1k_example1.wav \
  https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example1.wav
curl -sL -o experiments/vocal-samples/mir1k_example1REF.txt \
  https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example1REF.txt

# female example 2 (32.5 s, wide range) + ground truth
curl -sL -o experiments/vocal-samples/mir1k_example2.wav \
  https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example2.wav
curl -sL -o experiments/vocal-samples/mir1k_example2REF.txt \
  https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example2REF.txt

# male example 3 (27.3 s) + ground truth
curl -sL -o experiments/vocal-samples/mir1k_example3.wav \
  https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example3.wav
curl -sL -o experiments/vocal-samples/mir1k_example3REF.txt \
  https://raw.githubusercontent.com/vaaiibhav/mirexDatasetmir1k-2004/master/example3REF.txt

# ---- Wikimedia Commons (public domain, OGG → convert to WAV) ----
curl -sL -o experiments/vocal-samples/commons_caruso_la_donna_e_mobile.ogg \
  https://upload.wikimedia.org/wikipedia/commons/5/5a/La_Donna_E_Mobile_Rigoletto.ogg
curl -sL -o experiments/vocal-samples/commons_caruso_melba_soave_fanciulla.ogg \
  "https://upload.wikimedia.org/wikipedia/commons/9/9d/Enrico_Caruso_-_Nellie_Melba_-_La_Boheme_-_O_soave_fanciulla.ogg"

# convert OGG → WAV (44.1 kHz mono 16-bit, same format as MIR-1K)
ffmpeg -y -i experiments/vocal-samples/commons_caruso_la_donna_e_mobile.ogg \
  -ar 44100 -ac 1 -sample_fmt s16 experiments/vocal-samples/commons_caruso_la_donna_e_mobile.wav
ffmpeg -y -i experiments/vocal-samples/commons_caruso_melba_soave_fanciulla.ogg \
  -ar 44100 -ac 1 -sample_fmt s16 experiments/vocal-samples/commons_caruso_melba_soave_fanciulla.wav

# ---- LibriVox (public domain speech, MP3 → convert to WAV) ----
curl -sL -o experiments/vocal-samples/librivox_innisfree_wt_male.mp3 \
  https://archive.org/download/spc137_1411_librivox/spc137_lakeisleinnisfree_wt_128kb.mp3
curl -sL -o experiments/vocal-samples/librivox_elaine_ss_female.mp3 \
  https://archive.org/download/spc137_1411_librivox/spc137_elaine_ss_128kb.mp3
ffmpeg -y -i experiments/vocal-samples/librivox_innisfree_wt_male.mp3 \
  -ar 44100 -ac 1 -sample_fmt s16 experiments/vocal-samples/librivox_innisfree_wt_male.wav
ffmpeg -y -i experiments/vocal-samples/librivox_elaine_ss_female.mp3 \
  -ar 44100 -ac 1 -sample_fmt s16 experiments/vocal-samples/librivox_elaine_ss_female.wav
```

### Verification commands (as performed)

```bash
# HEAD status check (HTTP status recorded in the table above)
curl -sI -o NUL -w "%{http_code}\n" -L <URL>
```

### Files already downloaded & verified in `experiments/vocal-samples/` (2026-07-31)

| File | Bytes | Magic bytes |
|---|---|---|
| `mir1k_example1.wav` / `example2.wav` / `example3.wav` | 2,160,106 / 2,868,240 / 2,410,472 | `RIFF....WAVEfmt ` (PCM, mono, 44100 Hz, 16-bit) |
| `mir1k_example1REF.txt` / `2` / `3` | 26,051 / 35,070 / 28,494 | two-column `time \t pitch_Hz`, 10 ms hop |
| `commons_caruso_la_donna_e_mobile.ogg` | 1,541,593 | `OggS` |
| `commons_caruso_melba_soave_fanciulla.ogg` | 2,592,911 | `OggS` |
| `librivox_elaine_ss_female.mp3` | 1,730,472 | `ID3` |
| `librivox_innisfree_wt_male.mp3` | 1,265,669 | `ID3` |

---

## 5. 许可与归属（用于 `docs/testing/test-fixture-manifest.md`）/ License attribution notes

- **MIR-1K** — "free to use for research" per MIRLab (Hsu & Jang, 2010; `mirlab.org/dataset/public/MIR-1K.rar` — dead link as of 2026-07-31). Mirror repo `vaaiibhav/mirexDatasetmir1k-2004` (GitHub) has **no license file** — manifest should state: *"MIR-1K dataset (© MIRLab, free for research) via unofficial GitHub mirror; original clips 4–13 s, 44.1 kHz mono"* plus cite: Hsu, C.-L., & Jang, J.-S. R. (2010). *On the Improvement of Singing Voice Separation for Monaural Recordings Using the MIR-1K Dataset.* IEEE TASLP 18(2), 310–319.
- **Wikimedia Commons files** — Public Domain (`PD-old` / `PD-US` / `PD-Australia` / `PD-US Air Force`; marked CC-PD-Mark). Attribution per Commons file page; recordings: Enrico Caruso (1908/1913), Caruso & Nellie Melba (1907), USAF Singing Sergeants (1995).
- **LibriVox** — Public Domain (CC Public Domain Mark 1.0; `licenseurl` in archive.org metadata). Readers: Winston Tharp (male), Shakira Searle (female). Texts: W. B. Yeats, *The Lake Isle of Innisfree*; E. St. Vincent Millay, *Elaine*.
- All URLs verified 200 (2026-07-31) except explicitly noted (mirlab 404, CPDL 403, Zenodo records restricted).

## 6. 备注 / Caveats

- Commons/LibriVox files are OGG/MP3 — convert to WAV before feeding the YIN test pipeline (ffmpeg commands above). MIR-1K files are already WAV (44.1 kHz mono s16).
- Historical recordings (Caruso/Melba 1907–1913) contain surface noise and limited bandwidth (~78 rpm transfer) — good for robustness tests, not for clean pitch accuracy assertions; use MIR-1K for accuracy assertions.
- MIR-1K REF values are pitch in **Hz** with `0` for unvoiced frames; unvoiced frames were excluded from the stats above.
- Rate limiting: Wikimedia `upload.wikimedia.org` may return 429 on rapid consecutive requests (observed once) — add small delay/retry in download scripts.
