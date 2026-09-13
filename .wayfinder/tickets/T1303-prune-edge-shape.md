---
id: T1303
title: 剪枝边界深验的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 32 轮：spec 901 剪枝的边界组合（minItems≥total、阈值极小、与 memoization/预算共存）是否需要深验？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 32 轮 = effort #928 续 / spec 929 前插冲突→spec 931 / impl 684）：深验成立（薄加固轮）。`PruneEdgeDeepTest` 四场景：① minItems == total（全跑恰好触发但无剩余——run 完整不残缺）；② 阈值极小（0.01）首个非 pass 项即剪（ceil 语义边界）；③ 剪枝与 memoization 共存——memo 命中项计 passRate 但不绕过剪枝裁决（pruned 项不入 memo——止损语义不破坏记忆化）；④ 剪枝 run 的 Expectations 门在前独立拦截（组合序正确）。零生产变更预期（边界若实证缺陷按先例修）。
