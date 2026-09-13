---
id: T1492
title: 事实采集隔离硬化与计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1491
created: 2026-09-14
---

## Question

J 会话第 21 轮：隔离硬化与计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（FactCollectionStatsTest，内联 FactDefinition + 记账 FakeStore）：双定义命中 saved=2；judge 抛异常的定义被隔离（failures=1）且**其余定义照常采集**（saved 不减）；store save 抛异常入 failures；空判定全零；行为改进断言——异常定义存在时 afterTool 仍返回 CONTINUE 且后续定义事实已入账。定向 `mvn -pl buzhou-guard test -Dtest='FactCollectionStatsTest,GuardFactLoopEndToEndTest'` 绿（e2e 回归验证正常路径无扰）。
