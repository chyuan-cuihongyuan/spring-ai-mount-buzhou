# 847 — 数据集近重复读数

> 来源：H 会话第 48 轮 = effort #847 / [T1195](../../.wayfinder/tickets/T1195-dataset-near-duplicate.md) / [T1196](../../.wayfinder/tickets/T1196-dataset-near-duplicate-verify.md) / impl 600。
> 借鉴：Cleanlab 数据质量思想（≈10K star）。

## Problem

评估集被重复用例稀释：同义/复制用例让分数虚高——「评估集质量（重复率/最大簇）」无对账面。

## Solution

`DatasetNearDuplicateStats`（core.eval，纯函数）：

- **两两对账**：trigram Jaccard ≥ threshold 判近重复（714 同款）；并查集成簇（传递闭包）。
- **读数**：duplicatePairs（明细封顶 32 对）/largestCluster/uniqueRatio；条目封顶 200（截断+truncated 如实）。
- **防御**：threshold ∈ (0,1] fail-fast；脏条目跳过但计 itemsTotal。

## 兼容性

纯新增静态工具（条目由调用方自 EvalDatasetStore 采集）。

## 诚实边界

O(n²) 封顶截断；字符 trigram 非语义；传递闭包簇口径。
