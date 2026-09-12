# 603 — GCRA 平滑限流后端

> 借鉴：[redis-cell](https://github.com/andymccurdy/redis-cell) / Envoy GCRA rate limiter——TAT（理论到达时刻）匀速整形，无突发。
> 来源：F 会话第 4 轮 = effort #600 / [T856](../../.wayfinder/tickets/T856-gcra-backend-shape.md) / [T857](../../.wayfinder/tickets/T857-gcra-backend-verify.md) / impl 456。

## 背景

默认内存后端是令牌桶：容量即突发额度，开局可连打 capacity 次。对按平滑速率执法 RPM 的供应商（突发立即 429），需要「每个请求间隔 ≥ 60/capacity 秒」的整形器。GCRA 以单变量 TAT 实现无队列匀速（redis-cell / Envoy 同款算法）。

## 目标

`GcraRateLimitBackend implements RateLimitBackend`（opt-in 构造注入）：默认突发容忍 0 = 严格平滑；有限突发可配。

## 非目标

- 不改默认后端与 Redis 共享后端（零行为变化）。
- 不做 yml 装配面（编程注入先行；装配扩散另行）。
- 不做跨实例共享 GCRA（Redis GCRA 留雾区）。

## 设计

- τ = 60/容量（秒）；TAT per (model, dimension)，synchronized 单锁（与 TokenBucket 同并发风格）。
- 多单元：接受条件 `tat−now ≤ β+(n−1)τ`，接受后 `tat = max(tat,now)+nτ`；`consume` 强推 TAT（诚实超限——预检拒绝直至时间追上）。
- `available()` = 「此刻还能连发几个」：`floor((now+β−tat)/τ)+1` 封顶容量（GCRA 无离散 token，口径入档）。
- 未启用维度：恒拒/容量 0/等待 MAX_VALUE（对齐 InMemory，不抛）；`kind()="memory-gcra"`。
- nano 时钟注入（`LongSupplier`，测试确定性）。

## 测试

8 用例（[T857](../../.wayfinder/tickets/T857-gcra-backend-verify.md)）：步调、突发界、预检、超限恢复、等待秒数、available 口径、未启用维度、参数校验。

## 兼容性

纯增量新类；默认装配不变。
