# Spec 82 — 数据集指纹（effort #43）

> wayfinder map：`.wayfinder/maps/effort-43.md`（T319–T320）。借鉴：LangSmith dataset
> versioning；git tree hash 同思想。

## Problem Statement

run diff（spec 81）靠单侧项显形数据集漂移，但「就地改条目」（同 id 换 expected）
不可见：两次 run 对比会拿不同版本的数据当同版本比，STABLE 结论失真。

## Solution

`EvalDatasetStore.fingerprint(name)`：SHA-256 hex over 条目规范化序列
（`id\0input\0expected\0` 按 id 升序；内容寻址，名无关；不存在 = empty；空集 =
空序列哈希）。`EvalRunResult` 增 `datasetFingerprint`（9 参旧构造兼容；旧记录
null）；run 落盘/回读携带；`EvalRunDiff.DiffResult.datasetDrift` = 双侧指纹已知
且不等（旧记录单侧未知不误报）。

## User Stories

1. 作为评估作者，我要 diff 显形就地改项，所以 STABLE 结论可信任。
2. 作为红队，我要指纹内容寻址，所以同内容异名不误报漂移。
3. 作为既有用户，我要旧 run 记录不误报，所以升级零噪音。

## Implementation Decisions

- 指纹只覆盖 id/input/expected（溯源/时间戳非评估语义面，不进指纹）。
- 兼容面：EvalRunResult 9 参构造保留（既有调用零改动）。

## Testing Decisions

- 内容寻址：同内容异名等值、重读稳定、改即变、不存在 empty；
- run 携带落盘-回读等值；
- drift：同 id 集指纹不等 = true；相同 = false；单侧 null（旧记录）= false。

## Out of Scope

- 数据集快照副本存档；A/B run 指纹字段；指纹寻址存储。

## Further Notes

- ab.run 记录同字段是零成本扩展（fog 记账，需求证据后议）。
