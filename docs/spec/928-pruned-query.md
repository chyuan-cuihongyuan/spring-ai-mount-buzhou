# 928 — 评估 run 状态分布查询读面

> 来源：I 会话第 29 轮 = effort #928（[T1307](../../.wayfinder/tickets/T1307-pruned-query-shape.md) / [T1308](../../.wayfinder/tickets/T1308-pruned-query-verify.md) / impl 681）。spec 901 剪枝的查询面收口。

## 背景

spec 901 引入 `pruned` 项状态后，run 级「发生过剪枝」不可查——剪枝是算力止损事件，运维需要事后审计入口（哪些 run 被剪、剪了多少）。

## 目标

- `EvalQueryService.runsWithPruned()`：
  - 全量扫描 run 记录（既有 allRuns 前缀扫描口径），筛选 items 含 `pruned` 状态的 run；
  - 返回 `List<PrunedRunSummary>`：`record PrunedRunSummary(String runId, String datasetName, long prunedCount, int total)`，按 prunedCount 降序、runId 字典序 tie-break（输出稳定）；
- 纯查询面：state store 只读、零行为变化；损坏 run 记录跳过（既有容错口径）。

## 兼容性

纯增量：公共类新增方法 + 公共嵌套 record，零既有行为变化。
