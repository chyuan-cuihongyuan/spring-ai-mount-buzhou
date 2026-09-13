---
id: T1105
title: 指标新鲜度审计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

「指标不再有数」如何结构化发现？装饰器 vs 写点埋点、名字级 vs series 级、gauge 口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 3 轮 = effort #802 / spec 802 / impl 555）：`MetricFreshnessTracker` 装饰器（零委托变更）——counter/timer 刷新名字级 lastWrite；audit(now, staleAfter) 陈旧降序清单封顶 64；名字封顶 512+truncated；gauge 不追踪（连续量无写入语义）；Clock 注入。
