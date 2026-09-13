---
id: T1317
title: webhook 死信环形上限的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 37 轮：dead.* 死信前缀存量无上限（查询有 limit 但持续 markDead 无限累积）——环形上限是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 37 轮 = effort #937 / spec 937 / impl 689）：缺口成立。落点 `WebhookOutbox`：`MAX_DEAD_LETTERS=256` 常量 + `markDead` 写入前 `evictOldestDeadIfFull`（死信数达上限按 createdAt 升序丢最旧一条；O(n) n≤上限+1）。保留最新语义（排障价值优先）；渐进收敛无尖峰。既有 markDead/deadLetters/requeueDead 语义零变化。
