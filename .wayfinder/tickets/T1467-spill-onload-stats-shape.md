---
id: T1467
title: spill 回读命中率读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 9 轮：spill 回读命中率读面（PostgreSQL buffer hit-ratio 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 9 轮 = effort #1008 / spec 1008 / impl 761）：缺口成立——spill 回读路径 `OnloadHook.beforeTool`（写侧工具的长内容参数从盘回灌）零计数：回读成功（内容完好在盘）与回读失败（被逐/侵蚀/路径失效）不可分，spill 侵蚀率不可见。落点 buzhou-spill：OnloadHook 增 attempts/loaded/failed 三 AtomicLong（对内守恒：attempts == loaded + failed；空参数不计尝试）+ 新公共 record `SpillOnloadStats(attempts, loaded, failed)` + `stats()` 快照。命中率 = loaded/attempts 由消费方自算（诚实不设派生字段）。
