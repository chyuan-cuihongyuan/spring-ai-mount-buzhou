# Wayfinder Map — Buzhou fsck 巡检健康面（effort #548，E 会话第 48 轮）

> E 会话第 48 轮（538 巡检的健康面接入扩散轮——538 out-of-scope 兑现）。
> 勘察：538 巡检的 findings 只在 WARN 日志+计数——ops 标准健康读数面
> 缺失（细节不可从 /actuator 读）。

## Destination

`health.StoreFsckHealth implements BuzhouHealth`（mechanism=store-fsck，
观测面恒 UP——findings 是数据需关注非进程故障）：details = runs/
totalFindings/lastFindings（-1=尚未巡检）/skippedNotLeader。装配随
fsck.enabled。

## Notes

- 号段：spec 548 / T855-856 / impl-450。

## Out of scope

- findings>0 的 DOWN 语义（数据需关注非进程故障）。

## Tickets

- [x] [T855 健康面](../tickets/T855-fsck-health.md)
- [x] [T856 details 读数](../tickets/T856-fsck-health-details.md)
