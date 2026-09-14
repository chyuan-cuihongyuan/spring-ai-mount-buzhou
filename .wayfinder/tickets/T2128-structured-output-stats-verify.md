---
id: T2128
title: REASK 漏斗双守恒的 E2E 验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2127
created: 2026-09-14
---

## Question

如何证明漏斗计数与双守恒式在真实执行路径上成立？

## Resolution

**用户常设授权 AFK（可推翻）**

`StructuredOutputStatsTest` 四测全绿（`mvn -pl buzhou-core -am test`，E2E 走 chatForEntity 真实管线）：首过流（attempts=1/firstPass=1/firstPassRate=1.0）；REASK 恢复流（reasks=1/reaskParsed=1/rate=0.0）；双败流（failures=1+双守恒闭合）；空表/reset 哨兵（-1）。静态面测试前后归零防串扰。StructuredOutputEndToEndTest 回归 4 测绿。
