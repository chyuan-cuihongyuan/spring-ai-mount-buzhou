---
id: T1513
title: 审计收集器采集与持久化失败计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 32 轮：审计收集器采集与持久化失败计数读面（Splunk HEC ingestion stats 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 32 轮 = effort #1031 / spec 1031 / impl 784）：缺口成立——AuditTrailCollector（审计链事件收集器）onEvent 采集、store 持久化失败（ERROR 日志后继续）全程零计数：采集了多少审计事件、持久化失败多少次、当前追踪多少开启会话不可见——持久化失败连续即审计断链风险的第一信号。落点 buzhou-guard audit 包：实例级 collected/persistFailures 两 AtomicLong + 嵌套 record `AuditIngestStats(collected, persistFailures, openSessions)` + `stats()`（openSessions = 追踪中会话数）。实例级；嵌套类型不动 API 快照；采集/持久化/收尾行为逐位不变。
