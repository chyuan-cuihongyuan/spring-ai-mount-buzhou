---
id: T1297
title: 扩缩容建议缩容滞回的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 24 轮：BulkheadScalingAdvisor（spec 319）拒绝回零立即建议回落 1——HPA stabilization window（缩容防抖滞回）思想是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 24 轮 = effort #923 / spec 923 / impl 676）：缺口成立——瞬时拒绝抖动（单窗偶发）会被立即回落抹平历史，随后再拒又建议扩回（震荡）。落点 `BulkheadScalingAdvisor`：opt-in `stabilizeWindows` 参数（构造重载，默认 1 = 既有立即回落逐位不变）：回落 1 建议需**连续 stabilizeWindows 个窗口拒绝为零**才触发；期间保持上次非 1 建议。非 1 建议路径（扩容）即时性不变（扩容即时/缩容滞回——HPA 不对称语义）。建议序列计数器 `consecutiveIdleWindows`（实例字段，AtomicLong）。构造校验 ≥1。
