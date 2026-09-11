# Wayfinder Map — Buzhou spill 回读审计（effort #539，E 会话第 39 轮）

> E 会话第 39 轮（spill 模块首触轮；60/67 导出族同构）。勘察：readRange
> 回读（模型反复拉取溢出内容）无读数面：哪些证据被反复回读、读了多少、
> 有无完整性告警——排障与容量治理缺数据。

## Destination

`spill.ReadAuditTrail`（有界样本窗 128）：record(uri, bytes,
integrityWarning, at) + recent()/uriCounts()（降序）/totalReads/
totalBytes/integrityWarnings。接线：DiskSpillStore readRange 正常/告警
两路落样本（只观测零干预）+readAudit() 读数面。

## Notes

- 号段：spec 539 / T827–828 → 实际 T829-830 已用于 538；本票 T831–832 / impl-440。
- 借鉴源：60/67 导出族 + 416 分位族同构。

## Out of scope

- 跨进程聚合；自动容量动作；per-tool 归因。

## Tickets

- [x] [T831 审计轨迹原语](../tickets/T831-read-audit-trail.md)
- [x] [T832 store 接线](../tickets/T832-read-audit-store.md)
