---
id: T896
title: CompensatingBatch per-session 事务域的裁决（loop23 勘察发现的兑现）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Loop 23 勘察发现：CompensatingBatch.run 走 UnitOfWork 无参（全局锁）重载——全部 saga 全局串行，而 UnitOfWork SPI 早有 per-session 重载。归档族跨会话本可并行。怎么改？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 24 轮 = effort #600 / spec 623 / impl 476）：

1. `run(uow, sessionId, steps)` 重载：sessionId 非 null 走 per-session 事务（null = 全局既有语义）；补偿步与正向步同域（同锁序防交错）。
2. SessionArchiver.archive 传目标 sessionId（同会话已由 622 条目锁串行；跨会话并行）。
3. 其他 CompensatingBatch 调用方不动（全局域保守默认——各自按需迁移）。
4. JDBC/Redis UnitOfWork 的 per-session 语义由实现定义（契约测试背书）。
