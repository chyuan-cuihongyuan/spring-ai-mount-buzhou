# Wayfinder Map — Buzhou 数据集指纹（effort #43，50 轮自迭代第 8 轮）

> effort #43，延续 #42（T317–T318 / impl-228）。主线：run diff（spec 81）的单侧项
> 只能显形「增删」型数据集漂移；条目被就地改（同 id 换 expected）时对比会拿
> 苹果比橘子还报 STABLE。LangSmith dataset versioning 的最小内核 = 内容指纹。

## Destination

`EvalDatasetStore.fingerprint(name)`：SHA-256 over 条目规范化序列（内容寻址——
名无关）；`EvalRunResult` 增 `datasetFingerprint`（9 参旧构造兼容，旧记录 null）；
run 记录落盘/回读携带；`EvalRunDiff.DiffResult.datasetDrift`（双侧已知且不等才
true——旧记录不误报）。

## Notes

- 借鉴：LangSmith dataset versioning（内容哈希做版本标识）；git tree hash 同思想。

## Decisions so far

- 指纹只覆盖 id/input/expected（溯源字段与时间戳不进——非评估语义面）。
- 不做快照副本（指纹可验「同版本」，快照存档是另一维度——fog 记账）。

## Not yet specified

- 数据集快照副本（冻结版本存档）；A/B run 指纹（ab.run.<runId> 同字段可加）；
  Ragas 系指标；G-Eval rubric。

## Out of scope

- 沿用 #7–#42；指纹寻址存储（内容寻址只用于验证不用于存储）。

## Tickets

- [x] [T319 fingerprint + run 记录携带 + diff drift](tickets/T319-dataset-fingerprint.md)（impl-229）
- [x] [T320 3 例红队（内容寻址/落盘回读/drift 双侧语义）+ 文档收口](tickets/T320-fingerprint-close.md)
