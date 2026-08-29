# Spec 103 — 归档 TTL 治理（effort #65）

> wayfinder map：`.wayfinder65/MAP.md`（T383–T384）。spec 102 fog 后半场。
> 借鉴：S3 lifecycle expiration。

## Problem Statement

会话归档（spec 97）只进不出：冷层被当永久层用——合规期（如 90 天）过后归档
堆积占用 state store 容量，且无治理工具。

## Solution

`SessionArchiver.purgeExpired(ttl, now)`：删除归档时间（ArchiveEntry.archivedAt）
早于 `now - ttl` 的归档；逐条独立删除（单条 JSON 解析失败跳过不阻断批次——损坏
归档修复走手工删）；`ttl ≤ 0` = 显式全清语义；幂等（空批次返回 0）。运维经
cron/手动驱动（不自装调度——与 fsck/audit 同工具面纪律）。

## User Stories

1. 作为运维，我要归档到期自动让位容量，所以冷层不退化成第二热层。
2. 作为宿主，我要损坏条目跳过不阻断，所以单条坏数据不冻结整批治理。

## Implementation Decisions

- now 外注（Clock 注入面省——测试可控 + 与 SessionHistoryPolicy.expired(closedAt,
  now) 同签名纪律）。

## Testing Decisions

- 新鲜不删/快进过期全删；ttl=0 全清 + 幂等空批；损坏归档跳过且好的照删。

## Out of Scope

- autoconfig 定时键；按体积清理；purge 审计事件。

## Further Notes

- 与 ArchiveHealth（spec 102）组成归档治理闭环：可见 → 治理。
