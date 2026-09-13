# 818 — 限流自适应收紧器

> 来源：H 会话第 19 轮 = effort #818 / [T1137](../../.wayfinder/tickets/T1137-adaptive-rate-tightener.md) / [T1138](../../.wayfinder/tickets/T1138-adaptive-rate-tightener-verify.md) / impl 571。
> 借鉴：AWS SDK adaptive mode 客户端节流（aws-cli ≈16K 家族思想）。

## Problem

收到上游 429 后仍按原速率重发：重试风暴加剧拥塞（thundering herd 的客户端侧成因）。客户端在「明知上游在挤」时应主动少发。

## Solution

`AdaptiveRateTightener`（resilience.ratelimit）：

- **收紧**：onThrottled → 乘数 ×shrinkFactor（下限 minMultiplier）+重置保持窗。
- **恢复**：纯时间推导——保持期后每 recoverStepMillis ×recoverFactor 步进，封顶 1.0；无后台线程（确定性同参同值）。
- **接线**：调用方把 effectiveMultiplier 乘进 tryAcquire 的 amount（本类不改 RateLimitBackend SPI）。
- **有界**：模型封顶 32+truncated。

## 兼容性

纯新增；RateLimitBackend/ModelRateLimiter 零变更。

## 诚实边界

时间驱动恢复（非成功驱动——留位）；乘数信号不强制（策略域）；模型粒度（key 级归 VirtualKeys 族）。
