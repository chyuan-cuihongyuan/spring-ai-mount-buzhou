# 1031 — 审计收集器采集与持久化失败计数读面

> 来源：J 会话第 32 轮 = effort #1031（[T1513](../../.wayfinder/tickets/T1513-audit-ingest-stats-shape.md) / [T1514](../../.wayfinder/tickets/T1514-audit-ingest-stats-verify.md) / impl 784）。借鉴：Splunk HEC ingestion stats（采集/失败量是审计管道健康的第一水位）。与 R14/R15 同族：静默行为显形。

## Problem Statement

AuditTrailCollector（审计链事件收集器）把护栏/记忆裁决事件追加进 AuditChain 并即时落 AuditRecordStore，但零计数：采集了多少审计事件、持久化失败多少次（ERROR 日志后继续——失败连续即审计断链风险）、当前追踪多少开启会话不可见。

## 目标

- `AuditTrailCollector` 增量（buzhou-guard audit 包，实例级）：`collected`（通过 AUDITED_TYPES 过滤的采集数）/ `persistFailures`（store append 失败数——链不受影响语义不变）两 AtomicLong。
- 嵌套 record `AuditIngestStats(long collected, long persistFailures, int openSessions)` + `stats()` 快照（openSessions = 追踪中会话数）。
- 采集/持久化/收尾行为逐位不变。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按事件类型分桶（AUDITED_TYPES 15 型固定集合可做——留后续轮）。
- 持久化失败升级重试队列（语义变化另议）。
