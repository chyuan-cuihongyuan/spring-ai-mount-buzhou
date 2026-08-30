# Wayfinder Map — Buzhou 归档 TTL 治理（effort #65，50 轮自迭代第 30 轮）

> effort #65，延续 #64（T379–T380 / impl-249）。主线：**spec 102 fog 项「归档
> TTL 治理」后半场**——冷层不是永久层：合规期（如 90 天）过后归档应让位容量。

## Destination

`SessionArchiver.purgeExpired(ttl, now)`：删除归档时间早于 now-ttl 的归档（逐条
独立删、损坏跳过不阻断）；ttl ≤ 0 = 显式全清；返回删除数；幂等空批零操作。
运维经 cron/手动驱动（不自装调度——与 fsck/audit 同工具面纪律）。

## Notes

- 借鉴：S3 lifecycle expiration（冷层到期让位）。

## Decisions so far

- 不自装调度（运维驱动——与 StoreFsck/WebhookOutboxAudit 同工具面）。

## Not yet specified

- purge 的 autoconfig 定时键（需求证据后议）；按体积（非时间）的清理。

## Out of scope

- 沿用 #7–#64。

## Tickets

- [x] [T383 purgeExpired TTL 清理](tickets/T385-archive-purge.md)（impl-250）
- [x] [T384 3 例红队（过期删新鲜留/全清幂等/损坏跳过）+ 收口](tickets/T386-archive-purge-close.md)
