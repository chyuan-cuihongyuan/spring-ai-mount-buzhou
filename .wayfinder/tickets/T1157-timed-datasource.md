---
id: T1157
title: 计时 DataSource 的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

连接获取计时放 DataSource 装饰器还是 store 层？ring 复用还是新建？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 29 轮 = effort #828 / spec 828 / impl 581）：`TimedDataSource` 装饰器（装配侧显式包装）——两种 getConnection finally 计时进 810 StoreLatencyRing（操作名分离域）；其余方法纯委托；异常照记照抛。
