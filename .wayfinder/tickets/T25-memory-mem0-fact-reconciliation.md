---
id: T25
title: memory · Mem0 事实对账（ADD/UPDATE/DELETE/NOOP）
type: task
status: closed
assignee: "chyuan"
blocked-by: [T24]
epic: T13
created: 2026-08-13
---

**Status:** ready-for-agent（T24 闭合后）· **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#memory)

## What to build

九段摘要生成后，对每段候选事实跑对账 pass——与既有存储事实按语义近邻（embedding）比对，由模型裁决四态：`ADD`（无语义等价，新增）/ `UPDATE`（并入互补信息）/ `DELETE`（被新信息证伪）/ `NOOP`（无变化）。落点：摘要写侧 + SummaryStore 之上的事实索引。

## Acceptance criteria

- [ ] 端到端：长会话生成摘要后，重复事实被合并、矛盾事实被证伪/更新（评测式断言）
- [ ] 四态裁决可观测（事件/日志）
- [ ] 既有摘要保真评测不回归

## Blocked by

- [T24 · 增量摘要](T24-memory-incremental-summary.md)（复用 RunningSummary 状态）

## Resolution

已落地（Tier-1）。`SummaryFactReconciler`（buzhou-memory/summary）：合并后对「旧新皆非空」段跑对账 pass——模型按 `ADD/UPDATE/DELETE/NOOP` 四态裁决并输出对账后段正文（去重、矛盾以新证伪旧），应用后落库；事件 `memory.fact.reconciled`（section+四态计数；无 sink 时日志兜底）可观测。**韧性**：解析失败一律 NOOP（不落半成品）——既有 `SummaryEvaluationTest`/`SummaryIntegrationTest` 不回归（默认开）。`memory.fact-reconciliation` 开关。语义近邻 Tier-1 由模型裁决承担（向量 recall/embedding 索引为 Tier-2，spec 11 Out of Scope 已列）。spec 01 新增条目。测试：`IncrementalSummaryTest.reconciliationDeduplicatesAndRecordsBiTemporalValidity` / `unparseableReconcileResponseIsNoop`。
