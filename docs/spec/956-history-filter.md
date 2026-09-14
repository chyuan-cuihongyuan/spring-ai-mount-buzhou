# 956 — gate 历史按数据集过滤读面

> 来源：I 会话第 55 轮 = effort #956（[T1321](../../.wayfinder/tickets/T1321-history-filter-shape.md) / [T1322](../../.wayfinder/tickets/T1322-history-filter-verify.md) / impl 694 续）。spec 914 历史面的查询视图（多数据集共用 gate 实例时的定向观测）。

## 背景

多数据集共用一个 `EvalGate` 实例时，`history()` 混合全部判定——「只看 ds-X 的判定趋势」需宿主自行过滤。查询视图收口到门面。

## 目标

- `EvalGate.historyOf(String datasetName)`：环形史按 datasetName 精确匹配过滤（新→旧序保持）；null/blank fail-fast；
- 纯查询视图：环形史与既有 `history()` 零变化。

## 兼容性

纯增量：公共类新增方法，零既有行为变化。
