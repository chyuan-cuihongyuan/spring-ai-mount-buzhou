# Spec 426 — 模型并发舱（effort #426）

> wayfinder map：`.wayfinder/maps/effort-426.md`（T743–T744）。D 会话第 27 轮。

## Problem Statement

AgentBulkhead 限 per-agent 在飞 Turn、ModelRateLimiter 限 per-model
RPM/TPM（时间维速率）——**per-model 在飞并发**空白：单实例可同时打出
无限多并发请求（供应商并发配额分层，OpenAI 式并发 tier 是供应商侧
硬约束），超并发即 429 或账号限流。

## Solution

`resilience.concurrency.ModelConcurrencyLimiter` + 
`ModelConcurrencyAdvisor`（Resilience4j SemaphoreBulkhead 借鉴）：

- **Limiter**：per-model Semaphore（`limits` map 配置；未配置模型 =
  NOOP 零开销）；`acquireOrThrow(model)`——acquire-timeout（默认 0
  fail-fast）内取不到抛 `BuzhouException(QUOTA_EXCEEDED)`（AgentBulkhead
  同词汇）+ `buzhou.resilience.concurrency-rejected` 计数（model tag
  bounded）；`release(model)`；`inFlight()` 各模型当前在飞（观测）。
- **Advisor**（BaseAdvisor，链序 +660——rate-limit +650 内、
  resilience +700 外）：
  - call：acquire → try nextCall → finally release；
  - stream：acquire → `nextStream().doFinally(release)`——许可持续到
    流终结（含 CANCEL——在飞语义诚实）；
  - 链序语义：并发许可在重试包裹外获取一次、重试期间持续持有
    （在飞=逻辑调用占用，重试不重复扣）；拒绝在 nextCall 前抛→
    ResilienceAdvisor 不可见（不进重试分类）→ HookAdvisor 的
    onModelError 切面用户可兜底（RateLimitAdvisor 同语义）。
- yml：`buzhou.resilience.model-concurrency.{limits.<model>, 
  acquire-timeout}`；limits 非空才装配（Binder 预绑条件）。

## User Stories

1. 作为多模型宿主，我想给每个供应商模型设并发上限， so 单实例不打出
   超过供应商并发 tier 的请求（429 拒绝的上游根因消除）。
2. 作为运维，我想看到各模型当前在飞数， so 并发额度使用率可观测。

## Implementation Decisions

- 多实例诚实边界：每实例独立并发额度（无共享后端——限流族同注记）。
- 拒绝异常 NON_RETRYABLE 语义（QUOTA_EXCEEDED 词汇族）。

## Testing Decisions

- limiter：未配置 NOOP；cap=2 两取一拒一放；inFlight 观测；timeout
  路径抛。
- advisor E2E：阻塞模型双线程——A 持许可、B 拒（QUOTA_EXCEEDED），
  A 完成后 B 可行；流式完全消费后许可已还（下一次 call 即得）；流
  中途 dispose（CANCEL）许可也还。
- yml：limits 声明出 bean、未配置无 bean。

## Out of Scope

- AIMD 自适应（AdaptiveBulkhead 域）；跨实例共享；权重；排队。

## Further Notes

- 新公共类型 `ModelConcurrencyLimiter`、`ModelConcurrencyAdvisor`、
  `BuzhouModelConcurrencyProperties` 随轮 regenerate 快照。
