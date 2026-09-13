---
id: T1183
title: 会话空闲时长直方的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

空闲分布直方的边界与桶语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 42 轮 = effort #841 / spec 841 / impl 594）：`IdleDurationHistogram`——可配升序边界默认 1m/5m/15m/60m、恰达归右桶、AtomicLongArray 桶计数+total/longest+人话标签快照；负值忽略。
