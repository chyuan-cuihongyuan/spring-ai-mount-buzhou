---
id: T1090
title: 混合排序融合权重读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
605 权重声明是否生效不可见——加权重读数与融合计数吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 45 轮 = effort #744 / spec 744 / impl 645）：HybridSkillRanker 加 semanticWeight()/lexicalWeight()/fusedCount() 读数——构造期权重确认+RRF 融合完成次数（语义降级单路不计）。638 声明生效确认面同型。
