# Wayfinder Map — Buzhou store fsck 定时巡检（effort #538，E 会话第 38 轮）

> E 会话第 38 轮（341 选主扩散第三弹）。勘察：StoreFsck 对账面只有
> **手工触发**（run/repair）——定时巡检空白；341 选主门+Housekeeper
> 族模式（IdleCompaction 同型）可组合。

## Destination

`cleanup.StoreFsckHousekeeper implements SmartLifecycle`（IdleCompaction
同型：单线程 scheduleAtFixedRate+异常隔离+stop 关停）：周期 StoreFsck.run
（只读）→ findings>0 WARN+计数（不自动修复——repair 归手工面，删除
动作必须显式）；elector 缺席=无门单实例跑（ArchivePurgeJob 同语义）。
yml buzhou.fsck.{enabled, interval}（enabled 默认关 opt-in）。

## Notes

- 号段：spec 538 / T829–830 / impl-439。
- 借鉴源：fsck/scrub 定时巡检（存储面常规纪律）。

## Out of scope

- 自动 repair；健康面接入（312 规则可接计数）。

## Tickets

- [x] [T829 巡检 housekeeper](../tickets/T829-fsck-housekeeper.md)
- [x] [T830 yml 装配](../tickets/T830-fsck-assembly.md)
