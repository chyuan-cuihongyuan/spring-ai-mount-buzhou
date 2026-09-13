---
id: T1307
title: 评估 run 状态分布查询读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 29 轮：EvalQueryService 有 runs 查询——按状态分桶（pass/fail/error/pruned）的 run 级查询读面是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 29 轮 = effort #928 / spec 928 / impl 681）：缺口成立——spec 901 引入 pruned 状态后，「哪些 run 发生过剪枝」不可查（剪枝=算力止损事件，运维需要事后审计入口）。落点 `EvalQueryService` 新增 `runsWithPruned()`（全量扫描 run 记录，返回含 pruned 项的 run 摘要列表：`EvalRunSummary` 复用 + prunedCount 投影 record `PrunedRunSummary(runId, datasetName, prunedCount, total)`，按 prunedCount 降序）。纯查询面零行为变化；state store 前缀扫描既有口径。
