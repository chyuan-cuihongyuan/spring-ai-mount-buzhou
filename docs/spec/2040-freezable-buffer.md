# Spec 2040 — 可冻结分段缓冲（effort #2040，R41）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3181–T3182，impl 1591）。
> 借鉴：LSM memtable 不可变段切换——写满封冻、整段 flush。

## Problem Statement

spill 类缓冲的两难：逐条持久化写放大，而可变大缓冲又无法安全并发
快照（flush 期间写入使迭代失效）。LSM 的解法是「可变段写满封冻为
不可变段」——冻结后只读、快照安全，flush 以整段为单位。

## Solution

`FreezableBuffer<T>`（buzhou-spill，synchronized 小临界区）：

- `append(item)`：可变段满（segmentCapacity）即**自动封冻**开新段；
- `freeze()`：手动封冻（空段 no-op）——写入侧控制节奏；
- `drainFrozen()`：取走全部冻结段（flush 语义——取走清空；可变段
  保留未持久化）；
- `snapshot()`：冻结段序 + 可变段尾（追加序稳定）；
- 读数：frozenCount（待持久化积压面）/ mutableSize / totalSize；
- 契约：capacity ≥ 1、item 非空 fail-fast。

## User Stories

1. 作为 spill 作者，冻结段不可变——flush 期间并发快照安全不失效。
2. 作为写放大治理者，整段 flush 替代逐条写——段容量即批大小旋钮。

## Testing Decisions

- 容量 3 第四 append 触发封冻；手动封冻 + 空段 no-op；drain 取走两段
  内容精确 + 可变段保留 + 再 drain 空；快照 7 元素追加序；drain 后快
  照只含新写；容量 1 每 append 即冻；畸形两型 fail-fast。

## Out of Scope

- 不做并发 flush/写竞争调优（单锁口径）；不接 DiskSpillStore（接线
  归后续轮）。

## Further Notes

- 与组提交账面（O-1847）互补：那记组提交收益，这提供分段机制件。
