---
id: T1034
title: 共享事实冲突审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

聚合场景（导出/合并/多实例）同键异值静默共存——做冲突审计原语吗？自动裁决吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 18 轮 = effort #717 / spec 717 / impl 617）：`FactConflictAudit.audit(List<SharedFact>)` 纯函数——按键分组判 CONFLICT（同键异值，entries 全列 owner=value 证据齐备）/DUPLICATE（同键同值异 owner，重复发布信号）；value 等价用 Objects.equals（保守口径）；rows 按键字典序。不自动裁决（合并策略是业务语义）；不盯实时（快照审计口径）。mem0 冲突治理思想。
