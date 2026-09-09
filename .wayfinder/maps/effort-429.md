# Wayfinder Map — Buzhou 模型并发舱热更新（effort #429，D 会话第 30 轮·收口轮）

> D 会话第 30 轮（#426 扩散轮；收口轮）。勘察：模型并发舱（426）limits
> 构造期定死——供应商给提并发额度（或临时下调止血）要重启进程；
> AgentBulkhead.resize（320）与 RoutingWeightsHotReload（340）的
> 「BuzhouConfigRefreshEvent 重读 yml 热生效」模式未覆盖模型并发舱。

## Destination

`ModelConcurrencyLimiter.resize(newLimits)`（320 AgentBulkhead.resize
同语义：扩容 grow/缩容 shrink（reducePermits——在飞不受扰、瞬时可超
新限、释放自然收敛不抢占）；移除键摘舱（新 acquire=NOOP、在飞释放
无害）；逐键 WARN diff 留痕（417 价目热载同款审计面）+resized 计数）
+ `ModelConcurrencyHotReload`（ApplicationListener<BuzhouConfigRefresh
Event> 重读 `buzhou.resilience.model-concurrency.limits`→resize+
reloaded 计数+reloadCount() 观测）+ 装配拆三 bean（limiter 恒 exposed
——advisor 与热更新共享同一实例）。

## Notes

- 号段：spec 429 / T749–T750 / impl-402。
- 借鉴源：320/340 rebind 同模式（SRE 改 yml 发事件热生效不重启）。
- 纪律：acquire-timeout 构造期定死不热改（诚实边界——320 同注记）；
  收口轮兼归档（台账 30/30 + 记忆更新）。

## Out of scope

- acquire-timeout 热改；跨实例传播（各实例各自热载——rebind 族语义）；
- AIMD 自动伸缩（AdaptiveBulkhead 域）。

## Tickets

- [x] [T749 limiter resize 语义](../tickets/T749-model-concurrency-resize.md)
- [T750 热更新监听+装配三 bean](../tickets/T750-model-concurrency-hot-reload.md)
