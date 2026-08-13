---
id: T26
title: memory · 双时序事实有效性（标失效不删除）
type: task
status: closed
assignee: "chyuan"
blocked-by: [T25]
epic: T13
created: 2026-08-13
---

**Status:** ready-for-agent（T25 闭合后）· **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#memory)

## What to build

事实被取代时**标记失效（`valid_until`）而非物理删除**，保留 `valid_from`，支持时序回查与排障。基于 T25 的事实模型。

## Acceptance criteria

- [ ] 事实被取代时旧版标 `valid_until` 保留、新版 `valid_from` 生效
- [ ] 时序回查可取「某时点以为的事实」
- [ ] 排障可看事实演变
- [ ] 既有摘要/事实注入不回归

## Blocked by

- [T25 · Mem0 事实对账](T25-memory-mem0-fact-reconciliation.md)（复用事实模型）

## Resolution

已落地（Tier-1）。`BiTemporalFactLedger`（会话状态 `bitemp.summary.<SECTION>`，<=32 条/段）：对账应用 UPDATE/DELETE 时被取代段正文**标失效（valid_until）而非物理删除**、保留 valid_from；新版本同步开口生效（validFrom=now；评审修复：新版本此前未持久化）；`historyOf` 演变轨迹、`validAt` 时序回查。由 `InjectionViewProcessor`（sessionStateStore setter）与 `CompactNowTool` 双路径挂接。spec 01 新增条目。测试：`IncrementalSummaryTest.reconciliationDeduplicatesAndRecordsBiTemporalValidity`（旧版闭合+新版开口+时序回查）。
