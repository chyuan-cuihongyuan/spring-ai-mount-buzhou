---
id: T1305
title: 软截止预警集成的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 28 轮：spec 921 集成留位兑现——exec 内核软截止预警的形态裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 28 轮 = effort #927 / spec 927 / impl 680）：落点 `HarnessToolCallingManager`（spec 921 留位兑现）：`setSoftDeadlineWindow`（null=关）+ `awaitCompletion` 入口 `checkSoftDeadlineWindow`（首次进软窗 CAS 一次性 WARN + counter `buzhou.turn.soft-deadline`）+ `beginTurn` 复位 + `softDeadlineWarned()` 读面。派发/TIMEOUT 行为零变化（预警只观测不干预——K8s SIGTERM 预警语义）。
