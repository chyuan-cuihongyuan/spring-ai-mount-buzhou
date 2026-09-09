---
Type: task
Status: closed
---
## Question

归档 TTL 定时兑现：SmartLifecycle job + 默认关 autoconfig + 归档器兜底 bean。

## Resolution

done（2026-08-30）：impl-277；`retention/ArchivePurgeJob`（purgeOnce/listener/
自调度可关）+ `BuzhouArchiveProperties`（buzhou.session-archive.purge-* 三键
默认关）+ autoconfig 两 bean + 红队 3 例 + 启动校验回归。
