# Wayfinder Map — Buzhou 会话归档健康面（effort #64，50 轮自迭代第 29 轮）

> effort #64，延续 #63（T377–T378 / impl-248）。主线：**spec 97 fog 项「归档
> 容量治理」的前半场**——归档在册数先要可见（容量告警的前提），TTL 治理另议。

## Destination

`ArchiveHealth implements BuzhouHealth`（mechanism=session-archive）：恒 UP（观测
面——归档多非故障，容量治理走运维）；details = archivedSessions（countByPrefix
下推零值读）；`SessionArchiver.ARCHIVE_PREFIX` 升 public（健康面复用）；autoconfig
EndpointConfiguration 挂 bean。零新键。

## Notes

- 借鉴：健康段家族第五员（error-signatures/bulkhead/webhook-outbox/session-index）。

## Decisions so far

- 无归档 = 0 合法（不报 UNKNOWN——归档是运维动作非机制启用态）。

## Not yet specified

- 归档 TTL 治理（到期清 archive 键）；归档体积（字符数——需扫值，成本另议）。

## Out of scope

- 沿用 #7–#63。

## Tickets

- [x] [T379 ArchiveHealth + PREFIX 公共化 + 装配](../tickets/T381-archive-health.md)（impl-249）
- [x] [T380 2 例红队（生命周期计数/端点聚合）+ 收口](../tickets/T382-archive-health-close.md)
