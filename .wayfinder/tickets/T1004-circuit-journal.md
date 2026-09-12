---
id: T1004
title: 断路器变迁事件流读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

熔断变迁只在会话事件通道+当前值 gauge——进程级变迁史（跳了几次/多久恢复/谁反复跳）无查询面。加 journal 吗？要不要拒绝也记？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 3 轮 = effort #702 / spec 702 / impl 602）：加 `CircuitTransitionJournal`——变迁环形 64（dropped 计数）+per-model trips/recoveries/halfOpens 聚合+snapshot 不可变；内嵌 breaker 恒开零配置（变迁低频，读数面非行为面），`transitionJournal()` getter 暴露。拒绝不记（已有计数+事件，双口径是坑）。Resilience4j CircuitBreakerEvent 思想。
