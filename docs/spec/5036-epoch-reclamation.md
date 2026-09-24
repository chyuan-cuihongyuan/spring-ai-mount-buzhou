# Spec 5036 — Epoch-Based Reclamation 时代回收（effort #5036，S37）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6173–T6174，impl 2187）。
> 借鉴：folly/libcds EBR（Fraser 2004 时代守卫延迟回收思想）。

## Problem Statement

无锁结构内存管理的病：读完即 free（其他读者仍持引用——
引用-释放竞争）或靠 GC/锁（吞吐损失）——**时代守卫
延迟回收面**缺失。

## Solution

`EpochReclamation`（core/concurrent）：

- `enter()` 守卫钉住当前全局时代（计数+1），
  try-with-resources 即退（计数-1）；
- `retire(id)` 退休项记录当前时代；`advanceEpoch()` 显式
  推进全局时代（旧守卫钉旧值直至退出——确定性无墙钟）；
- `tryReclaim()`：仅回收「时代 < 全局时代 且 该时代及更早
  时代守卫清零」的项（不可能释放仍被引用的节点）；返回
  按时代升序+退休序（确定性）；
- 读数：currentEpoch/activeGuards/retiredCount；
- fail-fast：null/空 id、重复未回收 id、守卫双重关闭。

## User Stories

1. 作为无锁结构作者，读者持守卫期间节点绝不被回收。
2. 作为内存审计者，retiredCount 读数——回收欠账可见。

## Testing Decisions

- 守卫钉住 E0 退休项跨推进不可回收、退出后可回收；
  无守卫退休项推进即可回收；时代粒度分拣（E1 守卫只挡
  E1 项、E0 项照收）；回收序时代升序+退休序（a,b→c）；
  activeGuards 进出配平；双重关闭/重复退休 fail-fast。

## Out of Scope

- 不做真实并发线程调度（守卫进出为确定性 API 面）；
  不做 hazard pointer（同族异面）；不做对象句柄托管。

## Further Notes

- 与 SeqLock（spec 5007）同族不同面：奇偶序号乐观读 vs
  延迟回收生命周期。Wave 7 第一件。
- 里程碑：S37/50（74%）。
