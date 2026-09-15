# Spec 1810 — Prefetch 信用窗口（effort #1810，R11）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2821–T2822，impl 1411）。借鉴：
> RabbitMQ basic.qos prefetch / AMQP credit-based flow control——消费侧以未确认
> 配额反向节流生产侧，下游多快上游多快，免速率估计。

## Problem Statement

现有背压以速率口径为主（令牌桶/重试预算），缺**在飞上限**口径：工具/事件
消费侧「最多同时欠多少」没有配额闸——下游慢时上游照发，堆积在下游队列里
而不是停在门口。速率口径要估速率，估错就双双失真；在飞口径免估计、天然
自适应。

## Solution

`PrefetchCreditWindow`（core/backpressure，线程安全小临界区）：

- `PrefetchCreditWindow(capacity)`（≥1 fail-fast——0 是无限，语义不同）；
- `tryAcquire()` 满窗即拒（拒绝计入 totalExhausted——流控压力频率读数）；
- `release()` 确认归还信用（空窗归还 fail-fast——确认必须对应已取）；
- `stats()` 快照（capacity/inFlight/available/totalExhausted +
  utilization 在飞占比）。

## User Stories

1. 作为事件消费方，prefetch=2 窗口保证我最多欠 2 条——慢的时候压力停在
   门口，不堆在我院子里。
2. 作为运维者，totalExhausted 持续涨 = 下游跟不上（该扩消费方），
   utilization 贴 1 = 窗口配小了（该调大）。
3. 作为框架宿主，免速率估计的信用闸一行接入，快照直读。

## Implementation Decisions

- synchronized 小临界区（窗口账目是热点路径，锁内只有计数操作）。
- 配额静态（动态调整归宿主重建）；与速率口径互补不替代。

## Testing Decisions

- 满窗拒+耗拒计数+归还续流；容量 1 串行；初始快照；畸形（容量<1、空窗
  release）fail-fast。

## Out of Scope

- 不做动态配额；不接具体事件总线（接线归后续轮）。

## Further Notes

- 与 GcraRateLimitBackend 正交：那是速率上限，这是在飞上限。
