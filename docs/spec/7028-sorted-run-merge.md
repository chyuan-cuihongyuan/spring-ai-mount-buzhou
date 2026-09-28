# Spec 7028 — SortedRunMerge 键序合并（effort #7028，U29）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7257–U7258，impl 2280）。
> 借鉴：LevelDB/RocksDB 多路归并思想。

## Problem Statement

LSM 合并的病：只挑最新不清理（读放大）或全表覆盖写
（写放大）——**k 路归并+新覆盖旧+墓碑清理面**缺失。

## Solution

`SortedRunMerge`（core/metrics，静态工具面）：runs[0] 最新；
按 (key,runIndex) 归并——同键新游程覆盖旧值、TOMBSTONE
掩埋全部旧值且不输出；游程内同键取后写（LSM 语义）；
乱序/空游程 fail-fast；完全确定。

## Testing Decisions

- 新覆盖旧+墓碑掩埋手锚；200 轮随机游程 vs 旧→新放置
  TreeMap 圣像；乱序 fail-fast；游程内重复合法（后写）。

## Out of Scope

- 不做分层选择策略（LeveledCompaction 面）；不做区间
  集合并。

## Further Notes

- 与 ExternalMergeSort（6031）同族不同面：排序管道 vs
  新旧覆盖+墓碑清理。
- 里程碑：U29/50（58%）。
