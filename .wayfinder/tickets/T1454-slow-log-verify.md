---
id: T1454
title: 慢调用榜读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1453
created: 2026-09-14
---

## Question

J 会话第 2 轮：慢调用榜如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ToolSlowLogTest，AssertJ 同仓风格）：默认阈值下不达不入榜；configureThreshold 后超阈值入榜（toolName/durationMillis/failed 断言）；**严格大于**边界（恰好等于阈值不入、+1ns 入——Redis 口径）；FIFO 有界（灌 CAPACITY+8 留 32、新→旧序）；entries() 快照不可变；reset 清空。BeforeEach+AfterEach 双 reset（恢复默认阈值+清环）隔离静态污染。定向 `mvn -pl buzhou-core test -Dtest=ToolSlowLogTest` 绿 + HookedToolCallback 既有回归（ToolRunnerTest 域）绿。
