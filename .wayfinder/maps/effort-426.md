# Wayfinder Map — Buzhou 模型并发舱（effort #426，D 会话第 27 轮）

> D 会话第 27 轮（原 30 主题池「供应商并发配额」；换题注记：原拟
> 「Webhook 幂等键(Stripe)」勘察发现 forwarder 每请求已带
> X-Buzhou-Event-Id 幂等键+at-least-once 契约文档——撞已有能力弃）。
> 勘察：AgentBulkhead 是 per-agent 在飞 Turn 舱、ModelRateLimiter 是
> per-model RPM/TPM 时间维速率、AdaptiveBulkhead 是 per-agent AIMD——
> **per-model 在飞并发**（供应商并发配额，OpenAI 式并发分层）空白：
> 单实例可以同时打出无限多并发请求打挂供应商账号。

## Destination

`resilience.concurrency.ModelConcurrencyLimiter`（per-model Semaphore，
未配置模型=NOOP 零开销——AgentBulkhead 先例；acquireTimeout 默认 0
fail-fast；拒绝 BuzhouException(QUOTA_EXCEEDED)+计数 bounded tag；
`inFlight()` 观测）+ `ModelConcurrencyAdvisor`（BaseAdvisor，链序
+660——rate-limit(+650) 内、resilience(+700) 外：并发许可在重试包裹
> 之外获取一次、重试期间持续持有（在飞语义正确）；拒绝在 nextCall 前
> 抛→ResilienceAdvisor 不可见不进重试分类→HookAdvisor.onModelError
> 用户可兜底——RateLimitAdvisor 同语义；流式 doFinally 释放——许可
> 持续到流终结（含 CANCEL））+ yml `buzhou.resilience.model-concurrency.
> {limits, acquire-timeout}`（limits 非空才装配）。

## Notes

- 号段：spec 426 / T743–T744 / impl-399。
- 借鉴源：Resilience4j SemaphoreBulkhead + Uber concurrency-limits
  （供应商并发配额分层）；链序语义沿用 RateLimitAdvisor 文档口径。
- 纪律：多实例语义诚实（每实例独立并发额度——无共享后端；runbook
  注记同 54/57 族）；NOOP 零开销。

## Out of scope

- AIMD 自适应并发（AdaptiveBulkhead 域）；跨实例共享并发额度（Redis
  域）；per-request 差异化权重；排队（fail-fast 信号质量优先）。

## Tickets

- [x] [T743 ModelConcurrencyLimiter](../tickets/T743-model-concurrency-limiter.md)
- [T744 Advisor 链序/流式释放/装配](../tickets/T744-model-concurrency-advisor.md)
