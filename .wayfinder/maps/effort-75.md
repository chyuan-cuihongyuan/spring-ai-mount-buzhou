# Wayfinder Map — Buzhou 按数据集版本查 run（effort #75，50 轮自迭代第 40 轮）

> effort #75，延续 #74（T407–T408 / impl-259）。主线：spec 82（指纹）/100（快照）
> 有了版本锚，但查询面只能按名查——「这个冻结版本的全部历史 run」要宿主自己
> 翻。组合能力补查询闭环。

## Destination

`EvalQueryService.runsOfVersion(fingerprint)`：按内容指纹跨数据集名聚合（快照
与源同名不同版本天然分流）；`EvalRunSummary` 增 `datasetFingerprint` 列（8 参
旧构造兼容）；空/空白指纹 = 空结果（诚实不猜）；startedAt 倒序。

## Notes

- 借鉴：内容寻址查询（git ls-tree by hash 思想）；spec 82/100 组合收口。

## Decisions so far

- 摘要行带指纹（与 AbRunSummary 对齐——两侧同版本锚）。

## Not yet specified

- AB 面同款 runsOfVersion（abRuns 已带指纹——过滤面需求证据后议）。

## Out of scope

- 沿用 #7–#74。

## Tickets

- [x] [T409 runsOfVersion + 摘要指纹列](../tickets/T411-runs-of-version.md)（impl-260）
- [x] [T410 红队（跨名聚合/演化分流/空诚实）+ 收口](../tickets/T412-runs-of-version-close.md)
