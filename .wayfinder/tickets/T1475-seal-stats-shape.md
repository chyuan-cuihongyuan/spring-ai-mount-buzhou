---
id: T1475
title: 加密封存操作生命周期计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 13 轮：加密封存操作生命周期计数读面（age/OpenSSL ops 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 13 轮 = effort #1012 / spec 1012 / impl 765）：缺口成立——EncryptedSessionExport（spec 510）seal/open 全程无计数：封了多少、开了多少、**开失败多少**（密钥失配/密文篡改/载荷非导出 JSON 三条 DATA_CORRUPTION 拒绝路径）不可见——加密操作失败率是密钥轮换错配的第一信号。落点 core.session：EncryptedSession 增实例级 sealed/opened/openRejected 三 AtomicLong（open 失败三条拒绝路径都计 rejected；isSealed 静态判定不涉操作不计数）+ 嵌套 record `SealStats(sealed, opened, openRejected)` + `stats()`。实例态（hook bean 同理）非进程静态——无 reset 需求（会话域短生命周期）。
