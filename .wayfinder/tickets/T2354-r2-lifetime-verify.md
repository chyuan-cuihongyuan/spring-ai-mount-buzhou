---
id: T2354
title: R2 MCP 连接最大寿命的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2353
created: 2026-09-15
---

## Question

N 会话第 2 轮：如何验收？

## Resolution

`McpLifetimeRetireTest` 四断言（伪连接/伪工厂 + 可变时钟 + 手动驱动 retireExpiredOnce）：
① 未到寿零动作；② 到寿空闲重建一次（retiredCount=1、connectCount 增量、新条目可用）；
③ 在飞推迟（Latch 阻塞工具调用期间扫描只记 deferredRetireCount 不重建；释放后再扫重建）；
④ policy=null 时钟任意推进零动作。buzhou-mcp 模块测试全绿后单轮 commit。
