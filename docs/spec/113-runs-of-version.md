# Spec 113 — 按数据集版本查 run（effort #75）

> wayfinder map：`.wayfinder/maps/effort-75.md`（T409–T410）。spec 82/100 组合收口；
> git ls-tree by hash 思想。

## Problem Statement

指纹（spec 82）与快照（spec 100）给了版本锚，但查询面只按名：「这个冻结版本的
全部历史 run」（版本回归对比的底座）要宿主自己翻全部 run 过滤。

## Solution

`EvalQueryService.runsOfVersion(fingerprint)`：按内容指纹过滤（跨数据集名——
快照与演化前源同版本天然聚合；演化后分流）；`EvalRunSummary` 增
`datasetFingerprint` 列（8 参旧构造兼容——与 AbRunSummary 对齐同版本锚）；
空/空白指纹 = 空结果；startedAt 倒序。

## User Stories

1. 作为评估作者，我要一查冻结版本全部 run，所以版本回归对比零胶水。
2. 作为红队，我要演化分流正确，所以旧 run 归旧版本不混入。

## Testing Decisions

- live v1 + 快照 run 同指纹聚合、演化后 v2 分流；摘要行指纹等值；
  空/空白指纹空结果。

## Out of Scope

- AB 面 runsOfVersion；指纹反查数据集名。

## Further Notes

- 组合面：snapshotDataset → runsOfVersion → EvalRunDiff（spec 81）= 版本回归
  对比全链路。
