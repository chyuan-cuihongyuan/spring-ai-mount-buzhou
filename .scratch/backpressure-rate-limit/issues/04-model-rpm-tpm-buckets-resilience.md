# 04 — 模型 RPM+TPM 双桶: resilience 限流器 + 重试边界

**What to build:** `buzhou-resilience` 新增令牌桶限流器，挂在模型调用前切面（`ResilienceAdvisor` 同位，**先于重试/超时包裹**——限流裁决在最外层，重试的每次尝试重新过桶）。令牌桶按 modelName 分桶、单进程内存（`ConcurrentHashMap`，不引外部依赖）。**RPM** 调用前预检+扣减；**TPM** 调用后按实际 usage 记账（`chatResponse.getMetadata().getUsage()`，`ObservabilityAdvisor` 先例）+ 下次调用前预检，流式在流末尾聚合 usage 记账（accumulateStreamChunk 先例）；provider 不返回 usage 时 TPM 记 0 并留痕，不伪造估值。**重试边界**：自限流拒绝抛独立异常（如 `ModelRateLimitExceededException`），**不进入重试分类**直接上抛（provider 429 维持既有 RATE_LIMIT + Retry-After 重试不变）；确认两类异常在 `onModelError` Hook 切面均可被用户兜底。过载两档复用 01 词汇（queue 默认带超时 / fail-fast）。`backpressure.model-throttled`（modelName + 桶维度 + 等待时长）/ `backpressure.model-rejected` 事件进既有通道。配置进 `ResilienceProperties` 内嵌 rate-limit 组（前缀 `buzhou.resilience.rate-limit`，boxed null=不限）。e2e 复用 `ResilienceEndToEndTest` 形态（ScriptedChatModel 预排回复与 usage、调用计数断言、fastBackoff 助手）。

**Blocked by:** 01（两档策略词汇与事件语义；与 02/03 可并行）

**Status:** ready-for-agent

- [ ] e2e：低 RPM 下第 N 次模型调用排队/拒绝（ScriptedChatModel 调用计数 + seenPrompts 佐证）
- [ ] e2e：预排 usage 后 TPM 记账生效——下次调用被 TPM 桶拦（RPM 桶未满而 TPM 桶满）
- [ ] e2e：流式调用 TPM 在流末尾按聚合 usage 记账，语义与非流式一致
- [ ] 自限流拒绝不触发重试（调用计数不放大）；provider 429 重试语义回归不变
- [ ] `buzhou.resilience.rate-limit.*`（requests-per-minute / tokens-per-minute / queue-timeout / overload-policy）绑定生效，缺省 null=不限
- [ ] `backpressure.model-throttled` / `backpressure.model-rejected` 事件带 modelName、桶维度、等待时长
