# 歌曲数据来源调研：Key / 音域数据（M-1.x）

> 目的：为「按音域推荐歌曲」MVP（50–200 首）数据集（songId, title, artist, language, genre, originalKeyMidi, lowestMidi, highestMidi, tessitura, burden）寻找**真实、可引用、许可合规**的歌曲调性（key）与音域（vocal range）数据源。
> 调研方式：全部 API 均以真实 HTTP 请求实测（URL 见下），记录状态与返回内容；未实测的条目显式标注 `[未实测]`。访问时间：2026-07-31。
> 硬约束：不得批量虚构音域数据；所有 key 数据必须有可引用来源（PLAN §3.2）。

---

## 0. 结论摘要（TL;DR）

- **推荐主数据源（许可最干净，均已实测可用）**：
  1. **MusicBrainz（CC0）** —— 歌曲/艺术家/专辑元数据主干（标题、艺人、发行日期、语言），英文+中文覆盖都好（周杰倫 实测 880 条录音记录）。**注意：录音实体本身没有 key 字段**（实测响应字段：id/title/artist-credit/releases/length，无 key）。
  2. **AcousticBrainz（CC0）** —— **key/调性数据**：按 MusicBrainz 录音 MBID 索引，low-level 数据含 `key_key` / `key_scale` / `key_strength` / `tuning_frequency`（Essentia 算法分析）。**周杰倫《晴天》实测返回 G 大调（key_strength 0.903）**，中文歌覆盖可用但非全覆盖（另一首现场录音 MBID 返回 404）。2022 年 7 月起停止采集，数据冻结（7,564,215 首唯一录音），可全量 dump 下载。
- **Wikipedia（CC BY-SA 4.0）**：英文歌曲覆盖最好——en.wiki 4,091 篇文章含 "in the key of"、2,339 篇含 "vocal range"（实测样例：Faith 为 B 大调、人声 F♯4–G♯5；Run 为 A3–F5）。中文维基有少量（355 篇歌曲类含「大调」，如 thank u, next 记 D♭大调 + 人声 Ab3–Eb5），**周杰伦单曲条目缺失**（《晴天》无独立条目）。适合作为热门歌曲的逐首补充引用。
- **音域（lowestMidi/highestMidi）不在 MusicBrainz / AcousticBrainz 中** —— 必须按「引用来源优先 → 无法取得时显式 [推测] + 低可信度」的路径处理，见 §3。
- **不可用/不建议**：Hooktheory（API 文档 404）、songkeydb.com（连接失败）、Singing Carrots 音域库（登录/付费墙，无公开 API）、Spotify（需 OAuth 凭证，`[未实测]`，且 ToS 限制数据再分发）、Musicnotes/Tunescribers（无 API，仅可人工引用）、全民K歌/唱吧（无开放 API）。

---

## 1. API 实测记录

### 1.1 MusicBrainz Web Service v2 ✅ 可用（CC0）

- 实测 URL：`https://musicbrainz.org/ws/2/artist/?query=artist:"周杰倫"&fmt=json&limit=5`
  - 状态：HTTP 200（`read` 工具以浏览器 UA 请求，未带自定义 UA 亦成功；官方要求应用提供有意义的 UA）
  - 结果：`count: 1`；周杰倫 MBID `a223958d-5c56-4b2c-a30a-87e357bc121b`（country=TW，begin 1979-01-18，别名含 zh_Hans 周杰伦/zh_Hant 周杰倫，标签 mandopop/pop/chinese）
- 实测 URL：`https://musicbrainz.org/ws/2/recording/?query=artist:"周杰倫"&fmt=json&limit=3`
  - 状态：HTTP 200；`count: 880`（含现场、MV、demo、翻唱等变体，需按 release 过滤）
- 实测 URL：`https://musicbrainz.org/ws/2/recording/?query=recording:"晴天" AND artist:"周杰倫"&fmt=json&limit=3`
  - 状态：HTTP 200；`count: 10`；示例录音 MBID `1daf7867-18fe-4e09-893d-e41f72889cd3`（MV 版，316 秒），另有官方专辑（葉惠美 2003 / 尋找周杰倫 EP）内的录音版本，release 带 country/date/status 字段。
- **Key 结论（实测响应形状）**：recording 实体字段为 `id, title, artist-credit, releases, length, video, disambiguation` 等，**无 key 字段**；MusicBrainz 本体不存流行歌曲调性（work 属性中的 Key 仅古典作品偶见，不可依赖）。
- 官方限制（文档确认）：免费、**无需 API key**；必须提供有意义的 User-Agent；**速率 ≤ 1 请求/秒**（超限封 IP）；JSON 用 `&fmt=json`。
- 许可（官方文档确认）：**核心数据（artist/release/recording 等）CC0**；补充数据（tags/ratings/annotations）为 CC BY-NC-SA 3.0 —— **不要依赖 MB 的 tag 做 genre**。
- 全量数据：mbdump（data.metabrainz.org/pub/musicbrainz/data/）SQL 全库 dump，CC0。

### 1.2 AcousticBrainz ✅ 可用（CC0，key 主来源）

- 官网实测：`https://acousticbrainz.org/` —— 仍在运行（只读归档模式）。官方声明：2022 年停止采集（MetaBrainz 公告），**网站与 API 继续可用**；**全部数据 CC0**；统计：唯一录音 7,564,215 / 总提交 29,460,584，最后更新 2022-07-06。
- high-level API 实测（英文歌对照）：`https://acousticbrainz.org/f36d9818-019b-4379-ad13-5080feb9ad8a/high-level`（blink-182 "All the Small Things"）→ 200，含 gender/genre_dortmund/genre_rosamerica/mood_*/danceability 等模型（CC0）。**high-level 无 key**。
- **中文歌实测**：`https://acousticbrainz.org/1daf7867-18fe-4e09-893d-e41f72889cd3/high-level`（周杰倫《晴天》）→ 200，完整特征（gender: male 0.75、genre: electronic 等）—— **证明 AB 覆盖中文流行歌**。
- **low-level API 实测（key 所在）**：`https://acousticbrainz.org/1daf7867-18fe-4e09-893d-e41f72889cd3/low-level` → 200，JSON 末尾 `"tonal"` 节含：
  ```json
  "key_key": "G", "key_scale": "major", "key_strength": 0.902679383755,
  "tuning_frequency": 440.763
  ```
  → 《晴天》算法判定 **G 大调（强度 0.903）**，可直接映射 `originalKeyMidi = 67`（G4=67，C4=60）。
- 覆盖注意（实测）：`https://acousticbrainz.org/59e98295-e782-4bee-8a8c-3367f8b887b6/high-level`（周杰倫《黑色幽默》现场版）→ **404**。AB 仅覆盖用户曾用 Picard/Essentia 提交过的录音，中文歌覆盖为部分覆盖，**逐首必须验证**（404 = 无数据）。
- 批量获取：
  - **功能 CSV dump**（最适合建库）：`https://data.metabrainz.org/pub/musicbrainz/acousticbrainz/dumps/acousticbrainz-lowlevel-features-20220623/` —— 29,460,584 行，含 `tonal: key_key, key_scale, tuning_frequency` + rhythm（bpm 等）+ loudness，可直接按 MBID join。
  - JSON dump（zstd，30 个包 × 100 万文件）：`.../acousticbrainz-highlevel-json-20220623/`、`.../acousticbrainz-lowlevel-json-20220623/`；**样本 dump（10 万条）**：`.../acousticbrainz-sample-json-20220623/`（小规模测试用）。
  - API 参考：https://acousticbrainz.readthedocs.io/（GET `/{mbid}/low-level`、`/{mbid}/high-level`）。
- 可靠性：算法判定（Essentia 2.1），对流行歌 key 判定总体可靠，但**有概率性**——`key_strength` 可作置信度字段保留（建议 ≥0.6 才作为「直接引用」，否则降级为 [推测] 或换源）。

### 1.3 Wikipedia（en/zh）✅ 可用，逐首人工核验（CC BY-SA 4.0）

- 实测搜索（MediaWiki API）：`https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=insource:"in the key of" single&format=json` → **totalhits: 4,091**；样例：*Somewhere Only We Know*（A 大调）、*Save Me (Queen song)*（G 大调/D 大调）、*A Moment Like This*（E♭ 小调）。
- 实测搜索：`srsearch=single insource:"vocal range"` → **totalhits: 2,339**；样例：*Faith* (George Michael)「B 大调，人声 F♯4–G♯5」、*Run* (Snow Patrol)「A3–F5（引 Musicnotes/Hal Leonard）」。→ **部分英文歌曲条目同时给出 key + 音域，且注明乐谱出处，可直接引用**。
- 实测搜索（zh）：`https://zh.wikipedia.org/w/api.php?...&srsearch=歌曲 insource:"大调"` → totalhits: 355；样例：*小事情*（G 大调，引 musicnotes）、*謝謝，下一位*（D♭ 大调，**人声 Ab3–Eb5**）。
- 中文歌局限（实测）：zh.wiki 搜索「晴天 周杰倫」无单曲条目（只有艺人页/专辑页/演唱会列表）；周杰倫单曲 key/音域在维基基本缺失。
- 许可：**文本 CC BY-SA 4.0**（非 CC0）——引用时需署名+链接；对「事实性数据（调性）」逐条引用问题不大，但**不得整段复制**；音域数据在英文维基多源自 Musicnotes 乐谱（原出处本身有版权，作为事实引用可，批量抓取 Musicnotes 不可）。

### 1.4 Hooktheory TheoryTab ❌ 当前不可用

- 实测：`https://www.hooktheory.com/api/trendsite/docs` 与 `https://api.hooktheory.com/trendsite/docs` → **均 HTTP 404**。历史上需免费激活码，现文档已下线。**结论：不可用**（不作为数据源）。

### 1.5 songkeydb.com ❌ 不可达

- 实测：`https://songkeydb.com/` → 连接被关闭（socket closed）。**结论：不可用**。

### 1.6 Singing Carrots 音域库 ⚠️ 仅人工参考（登录/付费墙）

- 实测：`https://singingcarrots.com/song-database/` → 重定向到营销页；站点声称音域库覆盖 **75,000+ 曲目**（key + vocal range），但需登录/订阅，**无公开 API**，ToS 不允许批量抓取。**结论**：可作人工抽查/对照，不可入管线。

### 1.7 Spotify Web API ⚠️ 需凭证，`[未实测]`

- `GET /v1/audio-features/{id}` 返回 `key`（0–11 音级）、`mode`、`tempo` 等；需 OAuth Client Credentials（免费注册 App 即可，`[未实测]`——本机无凭证）。注意：**Spotify ToS 限制从 API 获取数据的再分发**；无音域字段。仅适合内部参考，不建议作为数据集来源。

### 1.8 其他候选（均为「不可用/不建议」结论）

| 源 | 状态 | 说明 |
|---|---|---|
| Echo Nest API | 已关停 | 2016 年被 Spotify 收购后 API 下线，功能并入 Spotify audio features |
| Mixed In Key | 商业软件 | 无公开数据集/API，不可引用 |
| KeyFinder（ibshillington/KeyFinder） | 开源工具 | GPLv3 本地调性检测；可对**自有音频**做诚实分析并注明「软件分析」 |
| Essentia extractor 静态二进制 | 开源（AGPL） | AB 官方推荐的自算方案（data.metabrainz.org 有各平台二进制） |
| Musicnotes / Tunescribers | 无 API | 乐谱产品页含调号/音域，仅可**人工逐首引用**（如维基那样注明出处）；禁止批量抓取 |
| 全民K歌 / 唱吧 | 无开放 API | 各曲有原调/升降调数据但不可程序化访问 `[未实测]` |
| kkbox Open API | 需审核 | 提供元数据/榜单，**无 key/音域**，价值低 |

---

## 2. 来源对比表

| 来源 | 许可 | key/调性 | 音域 | genre | 语言 | 访问方式 | 中文覆盖 | 可靠性 | 引用格式 |
|---|---|---|---|---|---|---|---|---|---|
| **MusicBrainz** | **CC0**（核心）；tags 等补充数据 BY-NC-SA | ❌ 无（实测） | ❌ | ⚠️ genre 实体可用（与 tags 同源，建议慎用） | ✅ release.language（zh/en 过滤） | REST API `ws/2`（免 key，≤1 req/s，需 UA）；mbdump 全量 SQL | ✅ 强（周杰倫 880 条录音；晴天 10 条） | 高（社区编目，需按官方 release 过滤变体） | 逐条：MusicBrainz, MBID `xxxx`, URL, 访问日期 |
| **AcousticBrainz** | **CC0** | ✅ **key_key/key_scale/key_strength**（low-level，Essentia 分析） | ❌ | ✅ high-level 多模型（genre_dortmund 等，CC0） | ✅ 按 MBID 关联 MB 的 title/language | REST API `/{mbid}/low-level|high-level`；**功能 CSV dump（29.46M 行，含 key_key/key_scale）**；样本 dump 10 万条 | ⚠️ 部分（《晴天》✅、《黑色幽默》现场 ❌404）——逐首验证 | 中高（算法判定，`key_strength` 保留为置信度） | 逐条：AcousticBrainz, MBID, key_key/key_scale, `key_strength`, URL, 访问日期 |
| Wikipedia（en） | CC BY-SA 4.0 | ✅ 部分（4,091 篇含 "in the key of"） | ✅ 部分（2,339 篇，如 Faith F♯4–G♯5、Run A3–F5） | ✅ 条目内 | ✅ 英文为主 | MediaWiki API / 页面 | ❌ 弱（英文歌强、中文歌弱） | 中高（内容引用乐谱出处，但需逐条核验） | 条目链接 + 访问日期（不整段复制） |
| Wikipedia（zh） | CC BY-SA 4.0 | ✅ 少量（355 篇歌曲类含「大调」） | ✅ 极少量（thank u, next: Ab3–Eb5） | ✅ | ✅ 中文 | MediaWiki API / 页面 | ⚠️ 极弱（周杰倫单曲无条目） | 中 | 同上 |
| Hooktheory | 商业/需激活码 | ⚠️ 曾有 | ❌ | — | — | **文档 404，不可用** | ❌ | — | — |
| songkeydb.com | 社区站 | ⚠️ 曾有 | ❌ | — | — | **不可达** | ❌ | — | — |
| Singing Carrots | 商业 | ✅ | ✅（75k 曲） | ✅ | ✅ | 登录/付费墙，无 API | ⚠️ 有中文 | 中（人工标注） | 仅人工抽查对照，不入库 |
| Spotify API | 商业 ToS | ✅ key/mode/tempo | ❌ | ✅ | ✅ | OAuth 凭证 `[未实测]`；ToS 禁再分发 | ✅ | 中高 | 不建议作为数据来源 |
| Musicnotes/Tunescribers | 商业版权 | ✅ 乐谱调号 | ✅ 乐谱音域 | ✅ | ✅ | 无 API，人工逐首 | ✅ 有中文 | 高（出版乐谱） | 维基式脚注：Musicnotes 产品页 + 日期 |
| KeyFinder / Essentia 本地分析 | GPLv3 / AGPL | ✅（自算） | ❌ | — | — | 本地运行 | 取决于音频 | 中（算法） | 注明「本地软件分析 KeyFinder vX, 日期」 |

---

## 3. 音域（lowest/highest/tessitura）的诚实获取路径

MusicBrainz/AcousticBrainz **均无音域字段**（实测确认）。按 PLAN「不得批量虚构」的约束，按以下优先级处理：

1. **(a) 引用乐谱来源的音域标注**：英文维基条目常直接给出（如 Faith: F♯4–G♯5；Run: A3–F5；thank u, next: Ab3–Eb5），其原始出处为 Musicnotes/Hal Leonard 乐谱。数据集内记 `lowestMidi/highestMidi` + 引用 URL（维基条目即可，注明「引 Musicnotes」）。**优点**：真实、可核查。**局限**：只覆盖热门英文歌，中文歌几乎没有。
2. **(b) 人工逐首查原调乐谱**：对数据集内中文歌，人工在 Musicnotes/Tunescribers/出版社简谱 中查该曲调号与音域并记录出处。MVP 50–200 首规模可承受，但每首需人工确认。
3. **(c) 显式 [推测] 派生值（低可信度）**：无来源时，用「key + 声部先验」派生（如男声流行主歌常用 G3–E5 区间、以 key 根音为中心 ± 一个八度），字段标记 `credibility: LOW`、`method: "key+genre prior"`，并注明「[推测]，非实测」。不得把 [推测] 值伪装成真实数据。
4. **(d) 本地音频分析（可选升级）**：有音频文件时用 Essentia extractor / KeyFinder 自算 key，或用 pYIN/CREPE（见 academic-research.md）分析人声主旋律得最低/最高音——**这是分析而非虚构**，引用格式注明工具与版本。

> 建议 MVP 策略：**英文歌走 (a)（维基/乐谱引用），中文歌走 (b)（人工查谱）为主、(c) 兜底并显式标记**；所有音域值在 schema 中带 `rangeSource`（url）/`rangeCredibility`（HIGH/MEDIUM/LOW）字段。

---

## 4. 推荐数据集构建路径（50–200 首）

```mermaid
flowchart LR
  A[选歌清单<br/>中英各半, 热门优先] --> B[MusicBrainz 搜索<br/>ws/2/recording?query=...]
  B --> C[取官方录音 MBID<br/>过滤 status=Official]
  C --> D[AcousticBrainz low-level<br/>/{mbid}/low-level 或功能CSV]
  D --> E{有 key_key?}
  E -- 是 --> F[key=key_key+key_scale<br/>originalKeyMidi=root<br/>保留 key_strength]
  E -- 否 404 --> G[Wikipedia 查 key<br/>或 KeyFinder 本地分析<br/>或标记 [推测]]
  C --> H[Wikipedia 查音域<br/>（英文歌优先）]
  H --> I{有音域?}
  I -- 是 --> J[lowestMidi/highestMidi<br/>rangeSource=条目URL]
  I -- 否 --> K[人工查原调乐谱<br/>或 [推测] LOW 可信度]
  F & J & K --> L[写入 song 表<br/>songId/title/artist/language/genre<br/>+ 逐条 sources 列]
```

要点：
- **批量 key 建议直接下载 AB 功能 CSV dump**（`key_key/key_scale/tuning_frequency/bpm`，29.46M 行，zstd 解压后按 MBID 索引 join），避免逐首 API 调用；样本 dump（10 万条）可先做可行性验证。
- MusicBrainz 调用遵守 1 req/s + UA；200 首全量检索成本极低（每首 1–2 次搜索 + 1 次 lookup）。
- 语言字段：MB release.language（zh/en）；genre：优先 AB high-level genre 模型（CC0）或自建，避开 MB tags（BY-NC-SA）。
- 每首歌在 `sources` 字段记：MB 录音 URL、AB key 值+强度+访问日期、维基/乐谱 URL（如适用）、[推测] 标记（如适用）。

## 5. 风险与注意事项

- **AcousticBrainz 是冻结项目（2022-07 停采）**：新歌/近期热门中文歌大概率无数据（实测现场版即 404）；数据不会增长。缺失时走 §3 的 (b)/(c)/(d)。
- **AB key 是算法判定**：保留 `key_strength`；强度 < 0.6 建议复核（听感/乐谱）或降级 [推测]。
- **Wikipedia 是 BY-SA 而非 CC0**：逐条引用链接+署名即可，禁止整段搬运；不要把维基音域当作「乐谱原件」引用，注明二手性质。
- **MB 补充数据（tags）BY-NC-SA**：genre 不要取自 MB tags。
- **音域值必须带来源与可信度**：无来源的数值一律 `[推测]` + LOW，保证数据集可审计。

## 6. 链接清单

- MusicBrainz API 文档：https://musicbrainz.org/doc/MusicBrainz_API （限速/UA）｜ 许可：https://musicbrainz.org/doc/About/Data_License
- MusicBrainz 周杰倫：https://musicbrainz.org/artist/a223958d-5c56-4b2c-a30a-87e357bc121b
- AcousticBrainz：https://acousticbrainz.org/ ｜ API 参考：https://acousticbrainz.readthedocs.io/ ｜ 数据页（schema 样例）：https://acousticbrainz.org/data
- AB dump：https://acousticbrainz.org/download （high/low JSON + 功能 CSV + 样本）
- AB《晴天》数据：https://acousticbrainz.org/1daf7867-18fe-4e09-893d-e41f72889cd3/low-level （key G major, strength 0.903）
- AB 停采公告：https://community.metabrainz.org/t/acousticbrainz-making-a-hard-decision-to-end-the-project/572828
- en.wiki 实测样例：Faith: https://en.wikipedia.org/wiki/Faith_(George_Michael_song) ｜ Run: https://en.wikipedia.org/wiki/Run_(Snow_Patrol_song)
- zh.wiki 实测样例：謝謝，下一位: https://zh.wikipedia.org/wiki/謝謝，下一位_(歌曲) ｜ 小事情: https://zh.wikipedia.org/wiki/小事情_(1世代歌曲)
- KeyFinder：https://github.com/ibshillington/KeyFinder ｜ Essentia：https://essentia.upf.edu/
- 关联文档：`docs/research/academic-research.md`（pYIN/CREPE 音高检测，用于 §3(d)）、`docs/research/source-register.md`（引用登记规范）
