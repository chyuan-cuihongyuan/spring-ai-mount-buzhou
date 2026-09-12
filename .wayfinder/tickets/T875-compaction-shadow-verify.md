---
id: T875
title: 影子干跑验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T874
created: 2026-09-12
---

## Question

影子干跑语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（CompactionShadowEvaluatorTest 4/4）：

- 干跑报告与直接压缩同口径（ids/chars 全等）且历史零变异。
- sweep 四档单调不减（0.25 ≤ 0.5 ≤ 0.75 ≤ 1.0）。
- 事件形状：candidateCount/reclaimedChars/evictRatio/applied=false。
- null compactor 构造拒绝。
