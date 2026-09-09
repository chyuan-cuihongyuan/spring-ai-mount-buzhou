# Spec 102 — 会话归档健康面（effort #64）

> wayfinder map：`.wayfinder/maps/effort-64.md`（T379–T380）。spec 97 fog 项前半场。

## Problem Statement

会话归档（spec 97）落 `__buzhou.archive__` 合成会话，但在册数不可见——归档堆积
（retention 批量清理期）无观测面，容量治理（TTL）缺前提。

## Solution

`ArchiveHealth implements BuzhouHealth`（mechanism `session-archive`）：恒 UP
（观测面——归档多非故障）；details = `{archivedSessions: N}`（countByPrefix 下推
零值读）。`SessionArchiver.ARCHIVE_PREFIX` 升 public（前缀单一事实源）。autoconfig
EndpointConfiguration 挂 bean。

## User Stories

1. 作为运维，我要归档在册数可见，所以批量归档期的容量趋势可告警。
2. 作为看板作者，我要健康段直连，所以归档水位一屏可见。

## Implementation Decisions

- 无归档 = 0 合法（非 UNKNOWN——归档是运维动作非机制启用态，与 bulkhead 的
  NOOP-UNKNOWN 语义区分入档）。

## Testing Decisions

- 恒 UP + 空 0 + archive/restore 生命周期计数 + 端点聚合段。

## Out of Scope

- 归档 TTL 治理；归档体积统计。

## Further Notes

- 健康段家族第五员；TTL 治理（fog）以本面为前提。
