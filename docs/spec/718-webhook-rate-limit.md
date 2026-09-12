# 718 — webhook 投递限速

> 来源：G 会话第 19 轮 = effort #718（借鉴 envoy local rate limit——令牌桶 + 上游维度）/ [T987](../../.wayfinder/tickets/T987-webhook-ratelimit-shape.md) / [T988](../../.wayfinder/tickets/T988-webhook-ratelimit-verify.md) / impl 521。

## 背景

WebhookEventForwarder 对 sink 投递无速率闸：事件风暴（批量会话结束/导出回调）全速打向下游消费者——慢消费者雪崩、连接池打爆、对端 5xx 连锁进死信。限速应发生在「重试状态机之前」——defer 不是失败。

## 目标

- `WebhookRateLimiter`（core/webhook）：令牌桶——`capacity`（burst）、`refillPerSecond`（持续速率）、时钟注入；`tryAcquire()` 无参（单 sink 语义；多 sink fanout per-URL 桶留位）；`deferredCount()` 观测。
- `WebhookEventForwarder.setRateLimiter` 可选（null = 关，默认零变化）：投递循环 attemptOnce 前取令牌——拒绝 → 记录**留在 outbox 原状**（attempts/nextAttempt 不动——defer 不是失败，不进重试状态机；下一 tick 令牌回填自然放行）+ 计数 `buzhou.webhook.ratelimit-deferred`；**整批全 defer 提前结束本轮**（防 deadline 内热旋）。

## 非目标

不做 per-URL/per-subscription 桶（单 sink 语义，fanout 联动留后续轮）；不做持久化令牌（进程内限速——多实例全局限速归共享后端既有面）。

## 测试

令牌桶节流（rate=2 一批 5 条恰 2 过 3 defer）、defer 不动 attempts、时钟推进回填放行、无 limiter 零回归、defer 指标事件。

## 兼容性

opt-in 纯增量；默认逐字节不变。
