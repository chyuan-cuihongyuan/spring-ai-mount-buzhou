---
id: T2167
title: 会话 spawn 统计读面（SessionSpawnStats）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 33 轮：spawn 冲突/抢占/活跃峰值读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：doSpawn 冲突拒绝与 steal 路径零读面；id 规划错误（同 id 冲突高发）静默。

形状裁决：SessionSpawnStats 进程级静态面（公共类）——attempts/successes/collisions/steals 漏斗+守恒 attempts=successes+collisions（steal 成功计入 successes）+activePeak spawn 时点采样口径显式+Snapshot/resetForTest；doSpawn 四点埋点只增记账。

Out of scope：全时段峰值；appId 分桶；容量闸计数（831 已有）。
