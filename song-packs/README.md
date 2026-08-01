# 歌曲包（Song Packs）

联网曲库扩展（BUG-018）：应用可从 HTTPS URL 下载歌曲数据包并导入本地曲库。
**仅下载歌曲元数据 JSON，不上传任何数据**（FR-PRIV-3 保持）。

## 格式

与内置数据集同构（`data/songs/src/main/assets/songs/mvp-songs.json`），
schema 见 `data/songs/src/main/resources/song-schema.json`。要点：

- 顶层为歌曲对象数组；
- 每首字段：`songId/title/artist/language/genre/originalKeyMidi/lowestMidi/highestMidi/tessituraLowMidi/tessituraHighMidi/rangeSpanSemitones/highNoteBurden/longNoteBurden/leapDifficulty/rhythmDifficulty/overallDifficulty/recommendedKeyShiftMin/recommendedKeyShiftMax/audioUrl(可空)/dataSource/credibility/dataVersion`；
- **一个包内所有歌曲 `dataVersion` 必须一致**（校验要求批次内版本一致）；
- 导入语义：与内置曲库版本不一致 → **事务替换整个曲库**（upsert + 差量清理 + 收藏保留）；
  因此"导入歌曲包"= 切换曲库（专攻方向），而非叠加。

## 托管方式

- 示例包：`zhou-jaylen-pack.json`（周杰伦 8 首扩展，dataVersion 1.1.0）；
- 应用内预设 URL 指向本仓库 raw（公开仓库）：
  `https://raw.githubusercontent.com/lzcjyx/matchsong/main/song-packs/zhou-jaylen-pack.json`
- 可托管于任何 HTTPS 静态站点（GitHub/Gitee raw、对象存储等），在设置页填入 URL 即可；
- 更新包：修改内容并**递增 dataVersion**，用户重新下载即替换。

## 内容与版权

- 包内为歌曲的**调性/音域元数据**（事实性数据，非歌词/音频/乐谱）；
- 示例包音域为"公开调性资料+听辨[推测]"（credibility MEDIUM）——**未人工标定，不宣称准确率**；
- 正式发布包前需自行确认内容合规（曲目元数据的商业使用、数据来源授权）；
- 建议按主题/歌手分包（如"周杰伦"、"国风"、"抽象"），保持每包 dataVersion 统一。

## 测试

- 下载器：`HttpSongPackFetcherTest`（MockWebServer：成功/404/超限）；
- 导入器：`SongPackImporterTest`（Fake 下载器 + Room In-Memory：替换语义/失败不落库）。
