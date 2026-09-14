---
id: T2133
title: 事务计量装饰器（InstrumentedUnitOfWork）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 17 轮：事务成功率读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：UnitOfWork 为 SPI（InMemory/JDBC/Redis 多实现）——埋点单实现只覆盖局部；装饰器方案可诚实覆盖全部实现（opt-in 装配）。事务域零读面。

形状裁决：InstrumentedUnitOfWork implements UnitOfWork（opt-in 装饰器）——begun/completed/failed/inFlight 守恒+失败异常类 Top 榜（简单类名降序典序、有界 8 并 OTHERS）+异常原样上抛+deleteSession 透传+Snapshot/resetForTest 实例级。

Out of scope：时延分位；会话分桶；CompensatingBatch 域。
