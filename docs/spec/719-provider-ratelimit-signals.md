# 719 — 供应商限流头前瞻读数

> 来源：G 会话第 20 轮 = effort #719（韧性族前瞻信号）/ [T1038](../../.wayfinder/tickets/T1038-provider-rl-signals.md) / [T1039](../../.wayfinder/tickets/T1039-provider-rl-signals-verify.md) / impl 619。

## Problem

限流韧性全是**事后**的：429 才解析 Retry-After、跳闸靠失败率累积。OpenAI/Anthropic 等主流供应商在每个响应都带 x-ratelimit-remaining-requests / -tokens 余量头——429 之前的拥挤信号白白丢掉。余量 5% 时还按正常速率硬打，下一秒就 429。

## Solution

- `ProviderRateLimitSignals`（resilience，纯静态解析原语）：
  - `parse(HttpHeaders)` → `Signals`——remainingRequests/limitRequests/remainingTokens/limitTokens（Long 可空）+resetRequests/resetTokens（Duration 可空，支持秒数与 HTTP-date 两种格式——Retry-After 同款解析口径）；
  - `requestUtilization()` / `tokenUtilization()`：`1 − remaining/limit`（缺 limit 或 remaining → NaN 诚实无值）；
  - `pressureLevel()`：NONE（<0.8）/ MEDIUM（≥0.8）/ HIGH（≥0.95）——消费端据此提前降速/换路；
  - 全字段 null-safe、畸形值跳过（fail-safe 不抛）；无任何相关头 → `empty()`；
  - 单响应快照口径——历史平滑/自适应降速是消费端职责（诚实边界）。

## User Stories

1. 前瞻降速：响应利用率连续 MEDIUM → 消费端放缓派发；HIGH → 主动切备模型——429 没发生就规避了。
2. 容量对账：limitRequests 读出供应商实际配额——与本地 buzhou.resilience.rate-limit 配置对齐。

## Implementation Decisions

- 头名按 OpenAI 通行约定（x-ratelimit-limit-requests / -remaining-requests / -limit-tokens / -remaining-tokens / x-ratelimit-reset-requests / -reset-tokens）；Anthropic 归一归宿主（不同供应商头名异——宿主可自行映射后调用 parse）。
- 放 resilience 根包（与 DefaultErrorClassifier 同域——错误分类是事后、本面是事前）。
- duration 解析支持 `1s`/`1m20s`/`2h` 复合与 HTTP-date。

## Testing Decisions

- 全量头解析各字段精确；利用率数学（1−3/10=0.7）；三级压力边界（0.799/0.8/0.95/0.96）。
- 缺 limit → NaN；无头 → empty()；畸形数字/reset 跳过不抛。

## Out of Scope

- advisor 拦截响应头自动接线（Spring AI 响应头暴露面差异大——宿主层接）。
- 自适应降速（压力级是信号——动作归消费者）。
- 多供应商头名归一表（后续按需）。

## Further Notes
