---
id: T2194
title: 采样漏斗分桶与归因的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2193
created: 2026-09-14
---

## Question

如何证明漏斗分桶归因与关闭语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`TurnSamplerHookStatsTest` 三测全绿（`mvn -pl buzhou-core -am test`）：100% 采样下漏斗四桶精确（written=1/short=1/empty=1——turnsSeen=2 不含空输入）；0% 早退不入漏斗（关闭零开销语义）；reset 归零。静态面前后归零防串扰。
