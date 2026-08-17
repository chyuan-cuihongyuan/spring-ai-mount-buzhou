# impl-125 — 时钟注入面

**What to build:** 熔断冷却与配额 UTC 日窗时间行为经可推进 Clock 驱动，测试零真实等待。

**Blocked by:** None

**Status:** done

- [x] ModelCircuitBreaker 三参构造 + 六处 Instant.now(clock)
- [x] SessionQuotaHook 三参构造 + todayKey 实例化 LocalDate.now(clock)
- [x] 测试：MutableClock 冷却 60s 推进 61s 即半开/跨 UTC 午夜配额重置（零 sleep）——resilience 86 绿
