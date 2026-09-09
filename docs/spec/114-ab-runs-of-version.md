# Spec 114 — AB 面按版本查 run（effort #76）

> wayfinder map：`.wayfinder/maps/effort-76.md`（T411–T412）。spec 113 fog 项（AB 同构）。

## Problem Statement

eval run 可按内容版本聚合（spec 113），A/B 对比不能：B 换版验收要「同基线版本的
历史对比集」（数据集演化后不同版本的胜率不可比），只能按名翻全量。

## Solution

`PairwiseEvalRunner.abRunsOfVersion(store, fingerprint)`（静态）：同指纹的历史
对比聚合（倒序沿用 abRuns 排序）；空/空白指纹 = 空结果。复用 abRuns 全量 + 内存
过滤（AB run 量级小——不做下推扫描）。

## User Stories

1. 作为验收作者，我要同基线版本对比集一查即得，所以 B 换版结论有可比历史。

## Testing Decisions

- v1 指纹聚合恰一条（演化后 v2 分流）；空指纹空结果。

## Out of Scope

- 版本对比集 diff 视图；下推扫描。

## Further Notes

- 组合：snapshotDataset → compare → abRunsOfVersion = 同基线验收全链路。
