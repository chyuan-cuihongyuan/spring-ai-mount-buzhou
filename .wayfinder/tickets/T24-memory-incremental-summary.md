---
id: T24
title: memory · 增量摘要（summarized_message_ids，不全量重摘要）
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T13
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#memory)

## What to build

RunningSummary 跟踪 `summarized_message_ids` / `last_summarized_message_id`，只把**新消息**折入既有摘要，避免全量重摘要的漂移累积与重复成本。

## Acceptance criteria

- [ ] 再次压缩只处理未摘要的新消息（不全量重摘要）
- [ ] 摘要代际连续、无重复漂移
- [ ] 既有九段摘要生成与评测不回归（`DefaultMicroCompactorTest`/`SummaryEvaluationTest` 绿）

## Blocked by

无 —— 可立即开工。（T25 依赖本片的 RunningSummary 状态。）

## Resolution

已落地（Tier-1）。`NineSectionSummary` 增加 `summarizedMessageIds` 消息级水位（与 `coversUpToTurn` 轮次水位双保险），`SummaryStoreBridge` 持久化 `__summarizedMessageIds`（截最近 400 条防膨胀）；`InjectionViewProcessor.toSummarize` 过滤 = 轮次区间 ∩ 未摘要 id——再次压缩只折入**新消息**，避免全量重摘要漂移与重复成本；代际连续单调（既有 `secondCompactionMergesWithPreviousGeneration` 保持绿）。spec 01 新增条目。测试：`IncrementalSummaryTest.secondCompactionOnlyFoldsNewMessagesByIdWatermark` / `alreadySummarizedIdsAreSkippedEvenWhenTurnWatermarkMisses`。
