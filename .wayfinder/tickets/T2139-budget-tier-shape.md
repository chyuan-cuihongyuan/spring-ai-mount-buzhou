---
id: T2139
title: 预算分档分类器（BudgetTierClassifier）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 20 轮（里程碑轮）：预算行动档位分类面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察辨义：预算闸（硬裁决）/1029 闸计数/806 分位推荐/817 多窗燃烧各有分工——缺无状态离线档位分类面。

形状裁决：BudgetTierClassifier 纯函数（core/budget）——classify(Map<String,long[]>)→TierReport(verdicts 饱和度降序典序+四桶计数+tightest(n) 派生)；Tier 闭集 GREEN/WARN(≥0.8)/HARD(≥1.0)/UNKNOWN(limit≤0 畸形、ratio=-1 不冒充)；常量显式。

Out of scope：告警联动；预测外推；动态阈值。
