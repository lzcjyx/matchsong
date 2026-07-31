#!/usr/bin/env python3
# M6.3-2 MVP 数据集构建：公开调性事实 + 音域推导（明确 [推测]/LOW 可信度）
# 来源：歌曲调性为广泛公开记录的事实（多来源交叉，见 docs/research/song-data-sources.md）；
# 音域为基于调性 + 歌手声部 + 原唱音频分析的推导值 [推测]。
import json, os

# MIDI note helpers
NOTE_OFFSETS = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'D#': 3, 'Eb': 3, 'E': 4, 'F': 5,
                'F#': 6, 'Gb': 6, 'G': 7, 'G#': 8, 'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11}

def note_to_midi(name):
    """'A4' -> 69；'C#5' -> 73"""
    # 字母部分：C, D, E, F, G, A, B (+ 可选 #/b 变音记号)
    i = 0
    while i < len(name) and name[i] in 'ABCDEFG':
        i += 1
    letter = name[:i]
    # 变音记号
    j = i
    while j < len(name) and name[j] in '#b':
        j += 1
    acc = name[i:j]
    octave = int(name[j:]) if j < len(name) else 4
    base = (octave + 1) * 12 + NOTE_OFFSETS[letter]
    if acc == '#':
        return base + 1
    if acc == 'b':
        return base - 1
    return base

def key_to_midi(key, scale):
    """'C' + 'major' -> 60 (C major)；minor -> 相对小调主音"""
    root = NOTE_OFFSETS[key]
    if scale == 'minor':
        return (4 + 1) * 12 + root  # 简化：小调主音根音（octave 4）
    return (4 + 1) * 12 + root

# 数据集：歌曲（调性为公开记录事实；音域为推导 [推测]）
# 字段：title, artist, lang, genre, key, scale, lowest, highest, tessLow, tessHigh, source
SONGS = [
    # 中文流行
    {"title": "晴天", "artist": "周杰伦", "lang": "zh", "genre": "流行", "key": "G", "scale": "major",
     "lowest": "G3", "highest": "C5", "tessLow": "A3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "七里香", "artist": "周杰伦", "lang": "zh", "genre": "流行", "key": "F#", "scale": "minor",
     "lowest": "F#3", "highest": "B4", "tessLow": "A3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "稻香", "artist": "周杰伦", "lang": "zh", "genre": "流行", "key": "F", "scale": "major",
     "lowest": "F3", "highest": "A4", "tessLow": "G3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "告白气球", "artist": "周杰伦", "lang": "zh", "genre": "流行", "key": "D", "scale": "major",
     "lowest": "D3", "highest": "B4", "tessLow": "E3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "小幸运", "artist": "田馥甄", "lang": "zh", "genre": "流行", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "C5", "tessLow": "E3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "演员", "artist": "薛之谦", "lang": "zh", "genre": "流行", "key": "C", "scale": "minor",
     "lowest": "C3", "highest": "G4", "tessLow": "D3", "tessHigh": "E4", "source": "公开调性资料+听辨[推测]"},
    {"title": "丑八怪", "artist": "薛之谦", "lang": "zh", "genre": "流行", "key": "Eb", "scale": "major",
     "lowest": "Eb3", "highest": "Ab4", "tessLow": "F3", "tessHigh": "F4", "source": "公开调性资料+听辨[推测]"},
    {"title": "光年之外", "artist": "邓紫棋", "lang": "zh", "genre": "流行", "key": "D", "scale": "major",
     "lowest": "D3", "highest": "D5", "tessLow": "F#3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "泡沫", "artist": "邓紫棋", "lang": "zh", "genre": "流行", "key": "F", "scale": "major",
     "lowest": "F3", "highest": "C5", "tessLow": "G3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "后来", "artist": "刘若英", "lang": "zh", "genre": "流行", "key": "Eb", "scale": "major",
     "lowest": "Eb3", "highest": "C5", "tessLow": "F3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "红豆", "artist": "王菲", "lang": "zh", "genre": "流行", "key": "F", "scale": "major",
     "lowest": "F3", "highest": "B4", "tessLow": "G3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "匆匆那年", "artist": "王菲", "lang": "zh", "genre": "流行", "key": "C", "scale": "minor",
     "lowest": "C3", "highest": "B4", "tessLow": "E3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "平凡之路", "artist": "朴树", "lang": "zh", "genre": "民谣", "key": "G", "scale": "major",
     "lowest": "G3", "highest": "D5", "tessLow": "A3", "tessHigh": "B4", "source": "公开调性资料+听辨[推测]"},
    {"title": "那些花儿", "artist": "朴树", "lang": "zh", "genre": "民谣", "key": "G", "scale": "major",
     "lowest": "G3", "highest": "B4", "tessLow": "A3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "突然好想你", "artist": "五月天", "lang": "zh", "genre": "摇滚", "key": "Bb", "scale": "major",
     "lowest": "Bb2", "highest": "F4", "tessLow": "C3", "tessHigh": "E4", "source": "公开调性资料+听辨[推测]"},
    {"title": "倔强", "artist": "五月天", "lang": "zh", "genre": "摇滚", "key": "E", "scale": "major",
     "lowest": "E3", "highest": "B4", "tessLow": "F#3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "海阔天空", "artist": "Beyond", "lang": "zh", "genre": "摇滚", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "D5", "tessLow": "E3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "光辉岁月", "artist": "Beyond", "lang": "zh", "genre": "摇滚", "key": "Eb", "scale": "major",
     "lowest": "Eb3", "highest": "C5", "tessLow": "F3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "凉凉", "artist": "杨宗纬/张碧晨", "lang": "zh", "genre": "流行", "key": "C", "scale": "minor",
     "lowest": "C3", "highest": "E5", "tessLow": "E3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "成都", "artist": "赵雷", "lang": "zh", "genre": "民谣", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "A4", "tessLow": "D3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "夜曲", "artist": "周杰伦", "lang": "zh", "genre": "流行", "key": "Am", "scale": "minor",
     "lowest": "A2", "highest": "E4", "tessLow": "C3", "tessHigh": "D4", "source": "公开调性资料+听辨[推测]"},
    {"title": "青花瓷", "artist": "周杰伦", "lang": "zh", "genre": "流行", "key": "G", "scale": "major",
     "lowest": "G3", "highest": "B4", "tessLow": "A3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "童话", "artist": "光良", "lang": "zh", "genre": "流行", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "C5", "tessLow": "E3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "勇气", "artist": "梁静茹", "lang": "zh", "genre": "流行", "key": "D", "scale": "major",
     "lowest": "D3", "highest": "B4", "tessLow": "F#3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "隐形的翅膀", "artist": "张韶涵", "lang": "zh", "genre": "流行", "key": "Eb", "scale": "major",
     "lowest": "Eb3", "highest": "C5", "tessLow": "G3", "tessHigh": "B4", "source": "公开调性资料+听辨[推测]"},
    {"title": "传奇", "artist": "王菲", "lang": "zh", "genre": "流行", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "B4", "tessLow": "E3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "十年", "artist": "陈奕迅", "lang": "zh", "genre": "流行", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "G4", "tessLow": "D3", "tessHigh": "E4", "source": "公开调性资料+听辨[推测]"},
    {"title": "浮夸", "artist": "陈奕迅", "lang": "zh", "genre": "流行", "key": "A", "scale": "minor",
     "lowest": "A2", "highest": "C5", "tessLow": "C3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "K歌之王", "artist": "陈奕迅", "lang": "zh", "genre": "流行", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "A4", "tessLow": "D3", "tessHigh": "F4", "source": "公开调性资料+听辨[推测]"},
    {"title": "你把我灌醉", "artist": "黄大炜", "lang": "zh", "genre": "流行", "key": "E", "scale": "major",
     "lowest": "E3", "highest": "C5", "tessLow": "G3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    # 英文流行/摇滚
    {"title": "Let It Be", "artist": "The Beatles", "lang": "en", "genre": "摇滚", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "G4", "tessLow": "E3", "tessHigh": "E4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Hey Jude", "artist": "The Beatles", "lang": "en", "genre": "摇滚", "key": "F", "scale": "major",
     "lowest": "F3", "highest": "C5", "tessLow": "A3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Yesterday", "artist": "The Beatles", "lang": "en", "genre": "流行", "key": "F", "scale": "major",
     "lowest": "D3", "highest": "E4", "tessLow": "E3", "tessHigh": "D4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Rolling in the Deep", "artist": "Adele", "lang": "en", "genre": "流行", "key": "C", "scale": "minor",
     "lowest": "C3", "highest": "C5", "tessLow": "E3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Someone Like You", "artist": "Adele", "lang": "en", "genre": "流行", "key": "A", "scale": "major",
     "lowest": "A3", "highest": "E5", "tessLow": "C4", "tessHigh": "B4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Hello", "artist": "Adele", "lang": "en", "genre": "流行", "key": "F", "scale": "minor",
     "lowest": "F3", "highest": "C5", "tessLow": "Ab3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Shape of You", "artist": "Ed Sheeran", "lang": "en", "genre": "流行", "key": "C#", "scale": "minor",
     "lowest": "C#3", "highest": "B4", "tessLow": "E3", "tessHigh": "G#4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Perfect", "artist": "Ed Sheeran", "lang": "en", "genre": "流行", "key": "Ab", "scale": "major",
     "lowest": "Ab3", "highest": "Eb5", "tessLow": "C4", "tessHigh": "C5", "source": "公开调性资料+听辨[推测]"},
    {"title": "Thinking Out Loud", "artist": "Ed Sheeran", "lang": "en", "genre": "流行", "key": "D", "scale": "major",
     "lowest": "D3", "highest": "A4", "tessLow": "F#3", "tessHigh": "G4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Bohemian Rhapsody", "artist": "Queen", "lang": "en", "genre": "摇滚", "key": "Bb", "scale": "major",
     "lowest": "Bb2", "highest": "F5", "tessLow": "C3", "tessHigh": "C5", "source": "公开调性资料+听辨[推测]"},
    {"title": "Don't Stop Me Now", "artist": "Queen", "lang": "en", "genre": "摇滚", "key": "F", "scale": "major",
     "lowest": "F3", "highest": "D5", "tessLow": "A3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Hotel California", "artist": "Eagles", "lang": "en", "genre": "摇滚", "key": "B", "scale": "minor",
     "lowest": "B2", "highest": "G4", "tessLow": "D3", "tessHigh": "E4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Wonderwall", "artist": "Oasis", "lang": "en", "genre": "摇滚", "key": "F#", "scale": "minor",
     "lowest": "F#3", "highest": "A4", "tessLow": "A3", "tessHigh": "E4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Billie Jean", "artist": "Michael Jackson", "lang": "en", "genre": "流行", "key": "F#", "scale": "minor",
     "lowest": "F#3", "highest": "E5", "tessLow": "A3", "tessHigh": "B4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Beat It", "artist": "Michael Jackson", "lang": "en", "genre": "流行", "key": "E", "scale": "minor",
     "lowest": "E3", "highest": "D5", "tessLow": "G3", "tessHigh": "B4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Hallelujah", "artist": "Leonard Cohen", "lang": "en", "genre": "民谣", "key": "C", "scale": "major",
     "lowest": "G3", "highest": "E5", "tessLow": "B3", "tessHigh": "C5", "source": "公开调性资料+听辨[推测]"},
    {"title": "Imagine", "artist": "John Lennon", "lang": "en", "genre": "流行", "key": "C", "scale": "major",
     "lowest": "C3", "highest": "E5", "tessLow": "E3", "tessHigh": "B4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Sweet Child O' Mine", "artist": "Guns N' Roses", "lang": "en", "genre": "摇滚", "key": "D", "scale": "major",
     "lowest": "D3", "highest": "C5", "tessLow": "F#3", "tessHigh": "A4", "source": "公开调性资料+听辨[推测]"},
    {"title": "Nothing Else Matters", "artist": "Metallica", "lang": "en", "genre": "金属", "key": "E", "scale": "minor",
     "lowest": "E2", "highest": "E4", "tessLow": "G2", "tessHigh": "D4", "source": "公开调性资料+听辨[推测]"},
    {"title": "My Heart Will Go On", "artist": "Celine Dion", "lang": "en", "genre": "流行", "key": "E", "scale": "major",
     "lowest": "E3", "highest": "E5", "tessLow": "A3", "tessHigh": "C5", "source": "公开调性资料+听辨[推测]"},
]

def main():
    records = []
    for i, s in enumerate(SONGS):
        low = note_to_midi(s["lowest"])
        high = note_to_midi(s["highest"])
        tl = note_to_midi(s["tessLow"])
        th = note_to_midi(s["tessHigh"])
        assert low <= high and tl <= th and tl >= low and th <= high, f"{s['title']} 音域非法"
        key_root = s["key"].replace('m', '') if s["scale"] == 'minor' else s["key"]
        key_midi = key_to_midi(key_root, s["scale"])
        span = high - low
        # 负担/难度推导 [推测]：跨度与高音位置
        high_note_burden = round(min(1.0, max(0.0, (th - 69) / 12)), 2)  # 主要音区相对 A4
        leap = round(min(1.0, max(0.0, span / 24)), 2)
        overall = round(min(1.0, max(0.0, (span / 24 * 0.6 + high_note_burden * 0.4))), 2)
        records.append({
            "songId": f"song-{i+1:03d}",
            "title": s["title"],
            "artist": s["artist"],
            "language": s["lang"],
            "genre": s["genre"],
            "originalKeyMidi": key_midi,
            "lowestMidi": low,
            "highestMidi": high,
            "tessituraLowMidi": tl,
            "tessituraHighMidi": th,
            "rangeSpanSemitones": span,
            "highNoteBurden": high_note_burden,
            "longNoteBurden": 0.5,
            "leapDifficulty": leap,
            "rhythmDifficulty": 0.5,
            "overallDifficulty": overall,
            "recommendedKeyShiftMin": -3,
            "recommendedKeyShiftMax": 3,
            "audioUrl": None,
            "dataSource": s["source"],
            "credibility": "LOW",
            "dataVersion": "1.0.0",
            "importBatchId": "mvp-001",
        })
    os.makedirs("data/songs/src/main/resources/songs", exist_ok=True)
    path = "data/songs/src/main/resources/songs/mvp-songs.json"
    with open(path, "w", encoding="utf-8") as f:
        json.dump(records, f, ensure_ascii=False, indent=1)
    print(f"WROTE {len(records)} songs -> {path}")
    langs = {}
    genres = {}
    for r in records:
        langs[r["language"]] = langs.get(r["language"], 0) + 1
        genres[r["genre"]] = genres.get(r["genre"], 0) + 1
    print("语言:", langs)
    print("风格:", genres)

if __name__ == "__main__":
    main()
