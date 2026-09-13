# effort #818 — 限流自适应收紧器

- 会话：H 会话 800 系第 19 轮 ｜ spec [818](../../../docs/spec/818-adaptive-rate-tightener.md) ｜ 票 [T1137](../tickets/T1137-adaptive-rate-tightener.md)/[T1138](../tickets/T1138-adaptive-rate-tightener-verify.md) ｜ impl571
- 借鉴：AWS SDK adaptive mode 客户端节流（aws/aws-sdk-java ≈6K 但 adaptive throttling 为 AWS CLI/SDK 家族通用思想，aws-cli ≈16K）——错误驱动收紧+渐恢复

## 勘察（排重）

- GcraRateLimitBackend/ModelRateLimiter：额度存取与策略——无「按上游 429 动态调放行」维。
- RetryBudget：重试预算（重试侧）——非放行乘数侧。
- grep -i `tighten|adaptive.*rate`：无命中（AdaptiveBulkhead 是并发自适应不同域）。

## 决定

`AdaptiveRateTightener`（resilience.ratelimit）：onThrottled 乘性收缩（×shrinkFactor，下限 minMultiplier）+重置保持窗；effectiveMultiplier(now) 纯时间推导——保持期内恒收紧值、其后每 recoverStepMillis ×recoverFactor 步进向 1.0 封顶；无后台线程（同参同值确定性）；模型封顶 32+truncated；五参数域校验 fail-fast。放行侧接线=调用方把乘数乘进 tryAcquire amount（本类不改 backend）。

## 测试

收紧+保持+步进恢复到 1.0（边界账修正：步从保持窗结束起算——2001ms 仍 0.5、2500ms 1.0）/反复 429 收缩到下限 0.05+确定性同值/多步渐恢复 0.25→0.5→1.0 精确/模型独立+null 忽略/封顶 32+truncated+超封顶未建态/五参 fail-fast——6 例绿。

## 诚实边界

乘数只是信号（接入 tryAcquire 归调用方/策略层——不改 backend SPI）；时间驱动恢复非成功驱动（成功信号语义留位）；模型域非 key 域（key 级收紧归 VirtualKeys 族）。
