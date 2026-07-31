#!/usr/bin/env python3
# M6.3-2 数据集构建脚本：MusicBrainz 搜索 + AcousticBrainz key 拉取
# 用法: python experiments/song-data/fetch_songs.py
import json, subprocess, time, urllib.parse, sys, os

UA = "MatchSongDev/0.1 (dev@example.com)"

def mb_search(query, entity="recording", limit=5):
    url = f"https://musicbrainz.org/ws/2/{entity}/?query={urllib.parse.quote(query)}&fmt=json&limit={limit}"
    out = subprocess.run(["curl", "-s", "-m", "20", "-H", f"User-Agent: {UA}", url],
                         capture_output=True, text=True)
    return json.loads(out.stdout or "{}")

def ab_low(mbid):
    url = f"https://acousticbrainz.org/{mbid}/low-level"
    out = subprocess.run(["curl", "-s", "-m", "20", "-H", f"User-Agent: {UA}", url],
                         capture_output=True, text=True)
    try:
        d = json.loads(out.stdout or "{}")
        t = d.get("tonal", {})
        if "key_key" not in t:
            return None
        return {
            "key": t.get("key_key"),
            "scale": t.get("key_scale"),
            "strength": round(t.get("key_strength", 0), 3),
        }
    except Exception:
        return None

# 歌曲清单（中文 20 + 英文 20；语言/风格标注）
SONGS = [
    # (搜索词, 语言, 风格)
    ("晴天 周杰伦", "zh", "流行"),
    ("小幸运 田馥甄", "zh", "流行"),
    ("告白气球 周杰伦", "zh", "流行"),
    ("七里香 周杰伦", "zh", "流行"),
    ("稻香 周杰伦", "zh", "流行"),
    ("演员 薛之谦", "zh", "流行"),
    ("丑八怪 薛之谦", "zh", "流行"),
    ("光年之外 邓紫棋", "zh", "流行"),
    ("泡沫 邓紫棋", "zh", "流行"),
    ("后来 刘若英", "zh", "流行"),
    ("红豆 王菲", "zh", "流行"),
    ("匆匆那年 王菲", "zh", "流行"),
    ("平凡之路 朴树", "zh", "民谣"),
    ("那些花儿 朴树", "zh", "民谣"),
    ("突然好想你 五月天", "zh", "摇滚"),
    ("倔强 五月天", "zh", "摇滚"),
    ("海阔天空 Beyond", "zh", "摇滚"),
    ("光辉岁月 Beyond", "zh", "摇滚"),
    ("凉凉 杨宗纬 张碧晨", "zh", "流行"),
    ("成都 赵雷", "zh", "民谣"),
    ("Let It Be The Beatles", "en", "摇滚"),
    ("Hey Jude The Beatles", "en", "摇滚"),
    ("Yesterday The Beatles", "en", "流行"),
    ("Rolling in the Deep Adele", "en", "流行"),
    ("Someone Like You Adele", "en", "流行"),
    ("Hello Adele", "en", "流行"),
    ("Shape of You Ed Sheeran", "en", "流行"),
    ("Perfect Ed Sheeran", "en", "流行"),
    ("Thinking Out Loud Ed Sheeran", "en", "流行"),
    ("Bohemian Rhapsody Queen", "en", "摇滚"),
    ("Don't Stop Me Now Queen", "en", "摇滚"),
    ("Hotel California Eagles", "en", "摇滚"),
    ("Wonderwall Oasis", "en", "摇滚"),
    ("Billie Jean Michael Jackson", "en", "流行"),
    ("Beat It Michael Jackson", "en", "流行"),
    ("Hallelujah Leonard Cohen", "en", "民谣"),
    ("Imagine John Lennon", "en", "流行"),
    ("Sweet Child O' Mine Guns N' Roses", "en", "摇滚"),
    ("Nothing Else Matters Metallica", "en", "金属"),
    ("My Heart Will Go On Celine Dion", "en", "流行"),
]

def main():
    out = []
    for query, lang, genre in SONGS:
        try:
            res = mb_search(query)
            recs = res.get("recordings", [])
            if not recs:
                print(f"MISS {query}")
                time.sleep(1.1); continue
            # 选第一个 Official 或第一个结果
            rec = recs[0]
            mbid = rec["id"]
            title = rec.get("title", "")
            artist = rec.get("artist-credit", [{}])[0].get("name", "") if rec.get("artist-credit") else ""
            key = ab_low(mbid)
            status = "OK" if key else "NO-KEY"
            print(f"{status} {query} -> {title} - {artist} key={key}")
            if key:
                out.append({
                    "query": query, "lang": lang, "genre": genre,
                    "mbid": mbid, "title": title, "artist": artist,
                    "ab": key,
                })
        except Exception as e:
            print(f"ERR {query}: {e}")
        time.sleep(1.1)  # MB rate limit 1/s

    os.makedirs("experiments/song-data", exist_ok=True)
    with open("experiments/song-data/raw-songs.json", "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    print(f"\nDONE: {len(out)}/{len(SONGS)} with keys -> experiments/song-data/raw-songs.json")

if __name__ == "__main__":
    main()
