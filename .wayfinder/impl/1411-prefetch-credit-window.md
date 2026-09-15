# impl 1411 — PrefetchCreditWindow 信用窗口（R11 = effort #1810 / spec 1810 / T2821-T2822）

**What**：`PrefetchCreditWindow`（core/backpressure，synchronized 小临界区）——
容量 ≥1 契约、tryAcquire 满窗即拒（totalExhausted 耗拒计数）、release 确认
归还（空窗归还 fail-fast）、stats 快照（capacity/inFlight/available/
totalExhausted + utilization）。

**Why**：RabbitMQ basic.qos / AMQP credit-based flow control 思想——在飞上限
口径的背压免速率估计：下游多快上游多快，慢的时候压力停在门口不堆在院子；
totalExhausted 持续涨=下游跟不上，utilization 贴 1=配额偏小。

**Verify**：`PrefetchCreditWindowTest` 4 用例全绿。

**Status**：done（2026-09-16）
