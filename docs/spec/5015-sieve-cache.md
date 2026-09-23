# Spec 5015 — SIEVE 缓存驱逐（effort #5015，S16）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6131–T6132，impl 2166）。
> 借鉴：SIEVE 缓存驱逐（2024 论文；Redis 社区/高星项目已跟进）。

## Problem Statement

缓存驱逐的病：LRU 每次命中搬移重排（热路径开销 + 扫描污染）
与 Clock（指针全局扫但无 visited 一次清位语义）——**FIFO 序 +
lazy promotion 访问位面**缺失。

## Solution

`SieveCache`（core/cache）：

- 插入序 FIFO 环 + 访问位：命中只置位**不重排**（lazy
  promotion——热路径零搬移）；
- 驱逐：指针自上次停点起扫——访问位=1 清位跳过、=0 摘除
 （一次保护机会；指针停在驱逐点之后）；
- 与 LRU 分叉显证：最老但已访问项可跨越后续插入持续存活
 （LRU 按新近度必先逐出）；
- 读数：size/hand/order（确定性审计面）；
- fail-fast：capacity≤0、null key/value。

## User Stories

1. 作为热缓存作者，命中零重排、扫描不洗出已保护项。
2. 作为审计作者，同操作序列同驱逐序（确定性可回放）。

## Testing Decisions

- 命中置位不重排（order 不变）；清位跳过一次后可驱逐；
  分叉场景（最老已访问项跨插入存活——LRU 必逐出对照）；
  容量 1 边界；capacity≤0/null fail-fast；确定性回放。

## Out of Scope

- 不做 TTL 联动；不做分片；不做过期惰性删除（respawn 面）。

## Further Notes

- 与 ClockSweepCache（spec 5009）同族不同面：使用计数衰减 vs
  访问位一次清位。Wave 3 第五件。
- 里程碑：S16/50（32%）。
