---
id: T1323
title: 剪枝 run 有效通过率口径的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 33 轮：spec 901 剪枝后 passRate 分母含 pruned 项（total=items.size()）——「2 pass / 5 total（3 pruned）」的 passRate=0.4 会把有效通过率 1.0 稀释。口径是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 33 轮 = effort #933 / spec 933 / impl 685）：口径缺口成立——passRate（既有字段，分母=total 含 pruned）与 effectivePassRate（分母排除 pruned）是两个都有用的口径，但后者缺失会让剪枝 run 的通过率被系统性稀释。落点 `EvalRunResult` 派生方法（record 加方法零破坏）：① `prunedCount()`——items 中 pruned 状态计数；② `effectivePassRate()`——`passed / (total − prunedCount)`（有效评估分母；全 pruned 约定 0.0 与空集纪律一致）。既有 passRate() 原样保留（总量口径——CI 硬门用原口径防剪枝刷分）。双口径显式并存。
