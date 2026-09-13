---
id: T1459
title: 维护窗历史读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 5 轮：维护窗历史读面（K8s cordon 事件史）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 5 轮 = effort #1004 / spec 1004 / impl 757）：缺口成立——MaintenanceGate（spec 205）只有 volatile 当前窗（isActive/window），begin/end 生命周期**过窗即逝**：「上次 cordon 是何时、为何、关了多久、期间拒了多少 Turn」不可回溯；Hook 的 micrometer 计数（buzhou.maintenance.blocked）是全局累计非按窗。落点 core.session：MaintenanceGate 增嵌套 record `HistoryEntry(window, beganAt, endedAt, refusals)` + 闭窗有界环（16，新→旧）+ `history()` 只读快照 + `noteRefused()` 窗内拒绝计数（MaintenanceGateHook 在既有 counter 旁同点补一行，包内可见）。实例态（gate 本是进程级 bean），begin/end 低频同步锁、拒绝路径 LongAdder 零争。
