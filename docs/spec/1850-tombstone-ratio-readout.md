# Spec 1850 — 墓碑占比读面（effort #1850，R51）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2901–T2902，impl 1451）。借鉴：
> LSM-Tree tombstone / Cassandra compaction——删除先落墓碑后靠压实物理
> 回收；占比是空间账与读放大账双面。

## Problem Statement

软删除（删除标记/过期标记待清扫）的积累只有总量记忆没有占比读数：「删
了但没真删」的存储背了多少死数据（空间账）、每次读跳过多少墓碑（读放
大账 = 1/(1−占比)）——不过阈即压的判定面缺位。

## Solution

`TombstoneRatioReadout`（core/cleanup，静态纯函数）：

- `ratioOf(liveEntries, tombstones)` → `Ratio(live, tombstones, ratio)`
 （占比 = 墓碑/全量，全空 0——无数据无墓碑语义）；
- `readAmplification()` 读放大倍数（占比 0.5 → 2 倍；全墓碑无穷）；
- `shouldCompact(threshold)` 压实阈判定（边界含上；默认阈 0.2 常量）。

## User Stories

1. 作为存储运维者，占比 0.5 + 读放大 2 → 一半存储背死数据、每读两键跳
   一墓碑——压实该跑了。
2. 作为容量规划者，占比爬升速度 = 删除/写入比——趋势外推该扩压实频。
3. 作为框架宿主，键口径自声明，纯读面零压实。

## Implementation Decisions

- 纯读不压实；全空 ratio=0 而非 -1（无数据是健康态不是无语义）。

## Testing Decisions

- 占比与放大（0.5→2×）+健康面（0.01→1.01×）；边界（全空 1×/全墓碑∞）；
  阈值含上；畸形三型 fail-fast。

## Out of Scope

- 不执行压实；不做分层数据（size-tiered/leveled 归未来静脉）。

## Further Notes

- 与 RetentionSweeper 正交：那是保留策略执行，这是软删除积累读面。
