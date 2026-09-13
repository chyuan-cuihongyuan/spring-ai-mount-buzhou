---
id: T1171
title: 尾采样决策台账的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

采样决策留痕与 eval 采样器如何辨义？聚合口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 36 轮 = effort #835 / spec 835 / impl 588）：`TailSamplingDecisionLog`——决策二值+原因开集（键封顶 16+溢出桶带决策维）；环 64+ringDropped+keptRatio；与 TurnErrorSampler eval 域正交；喂点手动。
