---
id: T987
title: webhook 投递限速的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

WebhookEventForwarder 对 sink 的投递无速率闸——事件风暴（批量会话结束）会全速打向下游 webhook 消费者（慢消费者雪崩/连接池打爆）。envoy local rate limit per upstream 怎么映射（OutboxRecord 无 URL——单 sink 语义）？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 19 轮 = effort #718 / spec 718 / impl 521）：`WebhookRateLimiter`（core/webhook）——令牌桶（capacity=burst、refillPerSecond 可配，时钟注入），`tryAcquire()` 无参（单 sink 语义——多 sink fanout 留位）；WebhookEventForwarder 增可选 `setRateLimiter`（null=关默认零变化），投递循环 attemptOnce 前取令牌：拒绝 → 该记录**留在 outbox 原状**（仍 due——下一 tick 令牌回填自然放行，tick 粒度即节流粒度，不碰重试状态机）+ 计数 `buzhou.webhook.ratelimit-deferred` + deferredCount getter；**整批全被 defer 时提前结束本轮**（防 deadline 热旋）。多 sink fanout 联动、per-URL 桶留后续轮。借鉴 envoy local rate limit（令牌桶 + 上游维度）。
