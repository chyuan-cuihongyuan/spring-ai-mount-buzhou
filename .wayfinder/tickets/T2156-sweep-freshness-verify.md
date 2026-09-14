---
id: T2156
title: 清扫新鲜度计数/间隔水位/失败分桶的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2155
created: 2026-09-14
---

## Question

如何证明计数、间隔水位与失败分桶正确？

## Resolution

**用户常设授权 AFK（可推翻）**

`RetentionSweepFreshnessTest` 五测全绿（`mvn -pl buzhou-core -am test`）：从未清扫 -1 哨兵；双次清扫 count/lastAt/stale/间隔 10 分钟精确；失败清扫单列（failures 非空）；**间隔水位不回退**（60 分大间隔后续 1 分不抬不降）；reset 全零。
