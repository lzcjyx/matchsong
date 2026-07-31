# MVP 数据集校验报告（M6.3-2）

- **日期：** 2026-07-31
- **数据集：** data/songs/src/main/resources/songs/mvp-songs.json
- **规模：** 50 首（zh 30 / en 20）
- **校验：** ImportRunner 全量校验 **0 错误**（MvpDatasetTest 回归锁定）
- **数据版本：** 1.0.0（mvp-001 批次）

## 校验项

- [x] JSON Schema 通过（SongSchemaValidator）
- [x] 重复检查（songId / title+artist+version）
- [x] 音高范围（lowest<=highest、tessitura 子集、MIDI 0-127）
- [x] 来源检查（dataSource 非空 + credibility 合法）
- [x] 版本检查（semver + 批次一致）
- [x] 回归测试（MvpDatasetTest 4 用例：规模/覆盖/来源/音域）

## 校验规则修正记录

- **originalKey 音域检查移除**：原调是歌曲调性（伴奏），与演唱音域独立（变调推荐正为此设计）。更新 SongImportValidator + 测试语义（M6.3-2）。
