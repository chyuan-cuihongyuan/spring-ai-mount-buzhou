---
id: T1082
title: 供应商限流信号 stats 接线的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
719 解析结果无处聚合——接进 ResilienceStats 吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 41 轮 = effort #740 / spec 740 / impl 640）：ResilienceStats 加 updateProviderUtilization/lastProviderUtilization（NaN 起始）+details 条件出现。单值快照（最近一次）；advisor 自动拦截沿用 719 边界。
