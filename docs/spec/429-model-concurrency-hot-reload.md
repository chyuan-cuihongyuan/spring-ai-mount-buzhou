# Spec 429 — 模型并发舱热更新（effort #429）

> wayfinder map：`.wayfinder/maps/effort-429.md`（T749–T750）。D 会话第 30 轮（收口轮）。

## Problem Statement

模型并发舱（spec 426）limits 构造期定死——供应商提并发额度或临时下调
止血都要重启进程；320/340 的 refresh 事件热生效模式未覆盖此面。

## Solution

`ModelConcurrencyLimiter.resize(newLimits)` + `ModelConcurrencyHotReload`
（320 AgentBulkhead.resize / 340 rebind 同模式）：

- **resize**：扩容 grow（补 permit）/缩容 shrink（reducePermits——
  在飞不受扰、瞬时可超新限、释放自然收敛、不抢占）；新模型键建舱；
  移除键摘舱（在飞释放无害、新 acquire=NOOP）；逐键 WARN diff（旧→新
  留痕——417 价目热载同款审计面）+ `buzhou.resilience.concurrency-
  resized` 计数。
- **HotReload**：`ApplicationListener<BuzhouConfigRefreshEvent>` 重读
  `buzhou.resilience.model-concurrency.limits` → resize；`reloaded`
  计数 + `reloadCount()` 观测。
- **装配拆三 bean**：limiter bean（limits 非空条件）恒 exposed——
  advisor RuntimeConfig 与热更新共享同一实例。

## User Stories

1. 作为 SRE，我想供应商提并发后改 yml 发事件即生效， so 不重启进程。
2. 作为值班，我想事故时把某模型并发限降到 1 止血， so 爆炸半径在线可调。

## Implementation Decisions

- acquire-timeout 构造期定死不热改（诚实边界——320 同注记）。
- Semaphore→可调子类（reducePermits 公开化——resilience4j 同法）。

## Testing Decisions

- resize：扩容后第二取成功；缩容低于在飞（3→1、在飞 2）→ 新取拒、
  释放两个后恢复放行（自然收敛）；移除键后 acquire NOOP。
- 热更新：StandardEnvironment+MapPropertySource 变更后发事件 → limits
  生效 + reloadCount 递增；再变更再发 → 二次生效。
- 装配：yml 声明三 bean 齐；未配置全无。

## Out of Scope

- timeout 热改；跨实例传播；AIMD 自动伸缩。

## Further Notes

- 新公共类型 `ModelConcurrencyHotReload` 随轮 regenerate 快照；
  收口轮兼归档（台账 30/30+记忆更新）。
