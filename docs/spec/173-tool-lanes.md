# Spec 173 — 工具泳道并发闸（effort #205）

> wayfinder map：`.wayfinder/maps/effort-205.md`（T539–T540）。借鉴：Hystrix 线程池
> 隔离舱——按资源类型分道，慢的一队饿不死快的一队。

## Problem Statement

Turn 并发许可（maxConcurrencyPerTurn）按<b>数量</b>发不按<b>类型</b>分：
批处理大查询（跑 30s）× N 并发就能吃光全部许可——快工具（查个词典型 50ms）
排不上队，整轮被慢工具的尾延拖死。资源隔离的经典解法是分道（lane）。

## Solution

`ToolLaneRegistry`（core/exec）+ `LaneLimitingToolCallback`：

- **泳道注册**：`lane(name, permits)` —— 命名泳道共享 Semaphore（同名单例）；
  宿主按工具资源画像分道（`slow-db: 2` / `fast-cache: 16`）。
- **装饰器**：`wrap(callback, laneName, registry, acquireTimeout)`——执行前取
  泳道许可（阻塞等待带超时，超时抛 IllegalStateException → harness 既有
  错误反馈词汇）；finally 归还（异常也归还）。
- 定义透传；与 retry/memo/transform 装饰器可叠加。

## User Stories

1. 作为宿主，慢查询工具标 `slow-db:2` 泳道——再多并发慢查询也最多 2 个在跑，
   快工具的许可不被挤占。
2. 作为运维，泳道排队超时文案直接指出哪个泳道满了——容量调参入口清晰。
3. 作为宿主，不标泳道的工具零变化（装饰器纯 opt-in）。

## Implementation Decisions

- 泳道许可获取 tryAcquire(timeout)（排队优先于拒绝——慢查询本就要等）。
- Registry 单例 map（同泳道名共享）；permits ≥1 校验。

## Testing Decisions

- 泳道并发封顶（3 线程并发过 lane-1 → 观测最大并发 1、串行完成）；
  异常路径归还（后续可再取）；不同泳道互不影响；获取超时抛错；定义透传。

## Out of Scope

- 泳道队列观测；yml 配置；泳道内优先级。

## Further Notes

- 隔离三层：agent 舱（84，Turn）/ 工具熔断（131，失败率）/ 泳道（本轮，资源分道）。
