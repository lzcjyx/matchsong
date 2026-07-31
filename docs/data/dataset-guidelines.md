# 歌曲数据采集规范（M6.3-1）

- **日期：** 2026-07-31
- **来源研究：** docs/research/song-data-sources.md（API 实测记录）

## 1. 来源类型与可信度分级

| 来源类型 | 描述 | 可信度 | 引用格式 |
|---|---|---|---|
| MB-AB | MusicBrainz 元数据 + AcousticBrainz 调性分析（key_key/key_scale/key_strength） | HIGH（key_strength ≥ 0.7）/ MEDIUM（0.4-0.7）/ LOW（<0.4） | `acousticbrainz.org/{mbid}/low-level` |
| WIKI | Wikipedia 歌曲条目（调性/音域引用） | MEDIUM（需条目明确标注） | `en.wikipedia.org/wiki/{Song}` |
| SHEET | 乐谱（Musicnotes 等）音域标注 | HIGH（若可得） | 乐谱 URL + 标注日期 |
| MANUAL | 人工听辨/查谱标注 | MEDIUM | 标注人 + 日期 + 方法 |
| DERIVED | 从调性 + 流派先验推导的音域 [推测] | LOW | 推导方法 + 参数 |

## 2. 字段来源要求

- `dataSource`：必填，来源标识（如 "musicbrainz/acousticbrainz"）
- `credibility`：按上表分级
- `dataVersion`：数据集版本（如 "1.0.0"）
- 音域（lowest/highest/tessitura）：优先 WIKI/SHEET 真实标注；无则 DERIVED 推导并标记 LOW 可信度
- 负担/难度字段：从音域跨度 + 调性推导 [推测]（方法文档化），MVP 后人工校准

## 3. 禁止行为

- 不得批量虚构音域（PLAN §12.3）
- 每首歌必须有来源或可信度声明（FR-SONG-2）
- 音域推导必须标记 [推测] 与 LOW/MEDIUM 可信度

## 4. 数据集构建流程

选歌清单 → MusicBrainz recording search（MBID + 语言）→ AcousticBrainz low-level（key）→ Wikipedia/乐谱（音域，可选）→ 生成 JSON → ImportRunner 全量校验（0 错误门禁）→ 合入。
