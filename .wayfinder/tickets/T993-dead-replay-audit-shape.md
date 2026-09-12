---
id: T993
title: 死信重放审计事件的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

replayDeadLetters()（spec 37）是运维敏感动作（把死信重新打入投递管线）——只有 INFO 日志：动作发生过没有、累计重放多少、频率如何，无任何可观测/审计面。审计事件怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 22 轮 = effort #721 / spec 721 / impl 524）：replayDeadLetters 增强——① 指标事件 `buzhou.webhook.dead-replayed`（delta=本次重放条数；requeued=0 不发——无动作无事件）；② 累计审计计数 getter：`replayCount()`（动作次数）/ `replayedCount()`（累计信件数）；③ 结构化审计日志升级（动作 + 条数 + 剩余死信数）。**诚实边界**：进程内审计面（指标+计数+日志）；写进 Merkle 审计链（spec 404）属 guard 域——webhook 域无链，链接线留位；actor 归属（谁按的按钮）在无宿主身份通道时不可得，留 fog。借鉴审计完整性惯例（敏感动作必留痕）。
