# Spec 539 — spill 回读审计（effort #539）

> wayfinder map：`.wayfinder/maps/effort-539.md`（T831–832）。E 会话第 39 轮。

## Problem Statement

readRange 回读（模型反复拉取溢出内容）无读数面：哪些证据被反复回读、
读了多少、有无完整性告警——排障与容量治理缺数据。

## Solution

`spill.ReadAuditTrail`（有界样本窗 128）：record(uri, bytes,
integrityWarning, at) + recent()/uriCounts()（降序）+ totalReads/
totalBytes/integrityWarnings；DiskSpillStore.readRange 正常/告警两路落
样本（只观测零干预）+ readAudit() 读数面。

## User Stories

1. 作为排障工程师，我想看哪些 spill 证据被反复回读， so 热点证据提示
   阈值/预览配置需要调整。
2. 作为运维，我想看完整性告警计数， so 落盘衰变被回读路径捕获。

## Implementation Decisions

- 只观测零干预；样本窗有界 128；重启清零（进程内观察面口径）。

## Testing Decisions

- readRange 落样本（uri/字节）；corruption 路径告警计数；窗口有界；
  per-uri 计数降序。

## Out of Scope

- 跨进程聚合；自动容量动作。

## Further Notes

- 新公共类型 `ReadAuditTrail`（嵌套 `ReadRecord`）随轮 regenerate 快照
  + api-surface.md 加行。
