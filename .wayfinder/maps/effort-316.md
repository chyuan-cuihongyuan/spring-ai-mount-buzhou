# Wayfinder Map — Buzhou 泳道 Redis 共享（effort #316，C 会话第 17 轮）

> C 会话第 17 轮。工具泳道（173）Semaphore 进程内——多实例部署时各实例
> 持整套泳道许可，慢工具道在实例间不再互斥（fog 227「Redis 泳道跨实例
> 共享族」项；共享族先例 54/57/125/200/315）。

## Destination

core SPI `LaneStateBackend`（tryAcquire(lane, permits)/release(lane) 原子
计数）+ `BackendLanePermit`（LanePermit 适配——超时轮询）+ 
`LaneLimitingToolCallback` 增加 LanePermit 重载（Semaphore 路径逐位不变）
+ Redis 实现（Lua INCR-with-cap / DECR floor-0）+ 装配 bean。

## Notes

- 号段：spec 316 / T623–T624 / impl-339。
- 借鉴：Redisson 分布式信号量（RPermitExpirable 语义收窄——无租约面）。

## Decisions so far

- 超时 = 调用侧轮询（200ms 步进——粗粒度泳道延迟容忍；Redis 阻塞语义无原生等价）。
- 崩溃泄漏诚实边界：进程崩未 release 的许可常驻——运维 reset(lane) 清零
  （无租约 TTL 面——复杂度不成比例，入档）。

## Out of scope

- 许可租约/TTL 自动回收；等待队列 FIFO（跨实例公平归 Redisson 全量面）。

## Tickets

- [x] [T623 LaneStateBackend SPI + BackendLanePermit 适配](../tickets/T623-lane-backend.md)（impl-339）
- [x] [T624 Redis Lua 后端 + callback 重载 + 回归](../tickets/T624-lane-redis.md)（impl-339）
