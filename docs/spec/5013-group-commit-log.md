# Spec 5013 — Group Commit 组提交（effort #5013，S14）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6127–T6128，impl 2164）。
> 借鉴：PostgreSQL/InnoDB group commit（WAL 一次 fsync 合并多事务）。

## Problem Statement

顺序化持久化的病：每事务一次 fsync（吞吐被设备延迟钳死）
或无持久上沿（哪些记录已落盘不可知）——**组内合并 +
持久上沿面**缺失。

## Solution

`GroupCommitLog`（core/recovery）：

- `append(record)`：记录进**当前组**并分配 LSN（严格递增）；
- `sync()`：一次持久化把当前组整体落盘——返回
  `GroupSync(records, fromLsn, toLsn)`；组空则空同步
 （records=0、上沿不变）；sync 后新 append 开新组；
- `durableUpto()`：持久上沿读数（单调不减——已落盘最大
  LSN）；
- fail-fast：null/空 record。

## User Stories

1. 作为审计日志作者，多事件合并一次落盘——吞吐不被设备
   延迟钳死。
2. 作为审计作者，同 append 序列同 LSN 轨迹（确定性可回放）。

## Testing Decisions

- 同组合并（3 append → 1 sync 覆盖 3、上沿=3）；sync 后新组
 （records=1、LSN 连续）；空组空同步（上沿不变）；LSN 严格
  递增与 durableUpto 单调；null/空 record fail-fast。

## Out of Scope

- 不做真实 IO/fsync（本件是分组持久语义面）；不做并行组
 （单组口径）；不做复制协商。

## Further Notes

- 与 GroupCommitAccounting（core/fs 记账面）同族不同面：
  统计读数 vs LSN 日志分组持久面。Wave 3 第三件。
- 里程碑：S14/50（28%）。
