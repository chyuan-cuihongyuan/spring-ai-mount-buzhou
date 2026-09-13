---
id: T1299
title: 观测存储水位读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 25 轮：InMemoryObservabilityStore 有逐出与丢弃计数（spec 13 有界纪律），但「当前存量 vs 上限」的水位读面是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 25 轮 = effort #924 / spec 924 / impl 677）：缺口成立——水位（Redis INFO 思想：used/peak vs maxmemory）是容量治理的第一问：「观测存量贴上限了吗」当前要人算。落点 `InMemoryObservabilityStore`（internal 包——非 API 面，无快照承诺）新增 `watermark()`：`record Watermark(int activeSessions, int maxSessions, long totalRecords, int maxRecordsPerSession, int sessionsEvicted)`（逐出累计已有计数直通）；activeSessions = 观测会话表大小、totalRecords = 各会话记录数合计。纯读面 synchronized 一致性；逐出/丢弃既有行为零变化。
