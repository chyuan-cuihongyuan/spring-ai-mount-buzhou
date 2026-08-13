---
id: T19
title: guard · 读写失败 on_fail 动词汇（FILTER/EXCEPTION/REASK）
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T15
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#guard)

## What to build

用业界 `on_fail` 失败动词作读写两侧统一语言，挂进 Hook 链：读侧默认 `FILTER`/`REFRAIN`（降级透传，映射既有「读降级」）、写侧默认 `EXCEPTION`（阻断，映射既有「写阻断」）、可恢复 schema 失败 `REASK`（错误回喂模型，有上界）。给既有读写失败非对称套上业界心智模型，**不改其语义**。

## Acceptance criteria

- [ ] 读侧校验失败按 `FILTER`/`REFRAIN` 降级透传（不阻断）
- [ ] 写侧校验失败按 `EXCEPTION` 阻断（不外流残缺产物）
- [ ] 可恢复失败按 `REASK` 把错误回喂模型并重试（有上界，不无限循环）
- [ ] 既有读写护栏端到端测试不回归

## Blocked by

无 —— 可立即开工。（与 T18 同在 Hook 链，建议串行以免冲突。）

## Resolution

已落地（Tier-1）。`OnFail` 枚举（core.hook，FILTER/REFRAIN/EXCEPTION/REASK）给既有读写失败非对称套业界心智模型、不改语义：读侧 `SpillOffloadHook` 可配 `offloadOnFail`（FILTER=既有降级透传默认；REFRAIN=保守拒答替代）；写侧恒 EXCEPTION（onload.failed/guard.tool.blocked 事件标注）；REASK=可恢复失败经 T16 错误回喂通道自纠重试，上界由 T17 有界 Turn 兜底。事件统一带 `onFail` 字段。spec 07 失败语义非对称节增词汇映射表。测试：`SpillOffloadHookTest.refrainOnFailReplacesDegradedResultWithRefusalNotice`、examples `OnFailReaskIntegrationTest`（坏参自纠一次成功 / 永不修正时预算内收尾）。
