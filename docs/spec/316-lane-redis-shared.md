# Spec 316 — 泳道 Redis 共享（effort #316）

> wayfinder map：`.wayfinder/maps/effort-316.md`（T623–T624）。借鉴：Redisson 分布式
> 信号量（RPermit 语义收窄——fog 227「泳道跨实例共享」项）。

## Problem Statement

工具泳道 Semaphore 进程内自持：多实例部署各持整套许可——慢工具道在实例
之间不再互斥，下游资源被 N 套泳道叠加打穿。

## Solution

**core**：SPI `LaneStateBackend`（`tryAcquire(lane, permits)` 原子
INCR-with-cap / `release(lane)` DECR-floor-0 / `reset(lane)` 运维清零）；
`BackendLanePermit`（超时轮询适配——200ms 步进）；`LaneLimitingToolCallback`
新增 LanePermit 重载（Semaphore 既有路径零变化）。

**Redis**：`RedisLaneStateBackend`（Lua 原子 acquire：cur+1 ≤ permits 才
INCR；release DECR floor-0；DEL reset）。键 {@code <prefix>lane:<名>}。

**装配**：store.type=redis 时供 bean（泳道名集声明本地——与 315 限额声明
同口径）。

## User Stories

1. 作为运维，4 实例共享 slow-db:2——下游数据库连接真上限 2 不是 8。
2. 作为宿主，单实例无感（Semaphore 路径默认零变化）。

## Implementation Decisions

- 崩溃泄漏：未 release 的许可常驻——`reset(lane)` 运维清零（租约/TTL 面
  复杂度不成比例，诚实入档）。
- 超时轮询步进 200ms（粗粒度泳道延迟容忍）。

## Testing Decisions

- core `BackendLanePermitTest`（fake 后端）：acquire/release 委托 / 满道
  超时 false / callback LanePermit 路径回喂正常。
- store-redis `RedisLaneStateBackendTest`（jedismock，EVAL 已证可用）：
  cap 边界 / release floor-0 / 跨实例合流 / reset。

## Out of Scope

- 许可租约；跨实例 FIFO 公平。

## Further Notes

- 共享族收口：限流（54）/ 熔断（57）/ 语义缓存（125）/ 舱（200 系）/
  key 配额（315）/ **泳道（本轮）**——fog 227 共享族清单清空。
