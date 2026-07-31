# 歌曲数据来源登记表（M6.3-1）

- **日期：** 2026-07-31
- **数据集：** data/songs/src/main/resources/songs/mvp-songs.json（50 首）

## 来源说明

### 调性（originalKeyMidi）
- 来源类型：**公开调性资料**（歌曲调性为广泛公开记录的音乐事实，多来源交叉）
- 引用参考：docs/research/song-data-sources.md（MusicBrainz/AcousticBrainz CC0 数据可交叉验证；API 实测部分歌曲）
- 可信度：MEDIUM（公开事实，但未逐首 API 复核）

### 音域（lowest/highest/tessitura）
- 来源类型：**DERIVED**（基于原调 + 歌手声部类型 + 原唱听感推导）
- 推导方法：歌手声部（男中/男高/女中/女高）典型音域 + 歌曲主旋律区间估计
- 可信度：**LOW**（dataSource 标注 "[推测]"）
- **诚实声明**：MVP 数据集音域为推导值，非逐首乐谱标注。M6 之后可逐首用乐谱/分析工具升级为 HIGH 可信度。

### 负担/难度（highNoteBurden 等）
- 来源类型：DERIVED [推测]（跨度 + 高音位置公式推导）
- 可信度：LOW

## 覆盖矩阵

| 维度 | 覆盖 |
|---|---|
| 语言 | zh 30 / en 20 |
| 风格 | 流行 34 / 摇滚 11 / 民谣 4 / 金属 1 |
| 歌手性别 | 男（周杰伦/薛之谦/朴树/陈奕迅/五月天/Ed Sheeran/The Beatles 等）+ 女（王菲/邓紫棋/田馥甄/Adele/Celine Dion 等） |
| 音区 | 男低（E2 海阔天空 Beyond）、男中、男高、女中、女高（E5 Someone Like You） |

## 校验状态

- ImportRunner 全量校验：**0 错误**（MvpDatasetTest 回归锁定）
- 数据版本：1.0.0

## 升级路径（后续）

1. 逐首用 AcousticBrainz low-level API 复核调性（MBID 已知可查）→ MEDIUM/HIGH
2. 音域用乐谱（Musicnotes 等）或音频分析（KeyFinder/pYIN）升级 → 高可信度
3. 中文歌优先补乐谱标注
