---
id: T1126
title: 观测管道内存限流器验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1125]
created: 2026-09-13
---

## Question

边界准入/并发守恒/归账防负如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 13 轮 = effort #812）：PipelineMemoryLimiterTest 6 例——60+40=100 恰达准入+第 1 字节拒收不记账/51>50 单项永不入账/release 归账+超发防负+预算完整恢复/零负权重恒过不计 admit/8×500 并发三口径守恒（admitted=1000 refused=3000 inFlight=1000）/fail-fast。
