---
id: T2149
title: 轮次限速 per-key 拒绝榜（TurnRateLimitHook 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 25 轮：轮次限速拒绝分布面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：TurnRateLimitHook 只有全局 counter+availableSnapshot 瞬时面；per-key 拒绝分布缺失（谁在被反复限速不可见）。

形状裁决：Hook 手术式增量——blockedByKeys 计数表（256 折叠纪律同 AgentBulkhead 拒绝表）+blockedSnapshot() 次数降序典序+resetBlockedForTest 清榜不清桶；beforeTurn 拦截路径单点记账，放行零动作，Block 语义逐位不变。

Out of scope：令牌分位；拒绝时刻环；集群聚合。
