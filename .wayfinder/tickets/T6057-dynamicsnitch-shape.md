---
id: T6057
title: R 会话 R29 动态 snitch 的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

副本池病副本怎么持续观测地动态降权？（spec 4028 / effort #4028 / R29）

## Resolution

**DynamicSnitchPenalty（core/policy）**：Cassandra dynamic snitch
——EWMA 平滑延迟 + 显著慢于最快 ×threshold 加固定罚分推队尾，
恢复 EWMA 回落自动免罚复用；未见副本零知识不罚 NaN 诚实。
与 TwoChoiceSelector 互补（持续观测 vs 单次选择）。
