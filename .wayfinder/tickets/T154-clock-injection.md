---
Type: task
Status: closed
---
## Question

限流/熔断冷却/配额 UTC 窗口/webhook 到期轮询全依赖系统时钟（全仓 main 仅 2 处注入 Clock），时间行为测试只能真实等待：时钟注入面切到哪些组件、什么形态（构造器/Builder 可选参）？

## Resolution

AFK 自决：注入面切熔断（ModelCircuitBreaker 六处 Instant.now→now(clock)）与配额日窗（SessionQuotaHook
todayKey 实例化 LocalDate.now(clock)）两组件——测试价值最高处先行；三参构造可选 clock、缺省 systemUTC
零变化。RateLimiter（nanoTime 单调域）/Advisor 退避 sleep/WebhookOutbox.due（已参数化）显式不注入
（诚实边界）。全仓统一 Clock bean 不做（避免过度装配面）。产 spec 41 §B + impl-125。
