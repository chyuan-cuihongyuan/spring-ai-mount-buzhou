---
id: T2132
title: 积压水位与阻塞计数面的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2131
created: 2026-09-14
---

## Question

如何证明水位单调、埋点接线与哨兵语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`EventBackpressureStatsTest` 四测全绿（`mvn -pl buzhou-core -am test`）：零哨兵；**直驱分发器**（容量 2 DROP_OLDEST 连发 6——水位 ∈[1,2]、blockedPushes=0）同包直调真实入队路径；record API 单调不回退+reset 归零；阻塞计数面独立断言。静态面前后归零防串扰。
