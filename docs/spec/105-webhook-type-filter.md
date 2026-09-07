# Spec 105 — webhook 订阅类型过滤（effort #67）

> wayfinder map：`.wayfinder/maps/effort-67.md`（T389–T390）。借鉴：GitHub/Stripe webhook
> 事件订阅面。

## Problem Statement

webhook 全事件投递：消费端只要 user.turn.completed 一类也被灌入全部事件——
outbox 容量与消费端幂等负担双吃；事件噪音还放大重试风暴。

## Solution

`WebhookEventForwarder.setIncludeTypes(Collection<String>)`：空集 = 全投递（默认
零变化）；命中才入队（被滤事件不占 outbox 容量——容量语义只属于待投事件）+
计数器 `buzhou.webhook.filtered`。装配经 Binder 读 `buzhou.webhook.include-types`
（List<String>；缺省空 = 全投递）。BuzhouWebhookProperties record 不扩（构造链
兼容——独立 setter 注入面）。

## User Stories

1. 作为消费端，我只收关心的事件类型，所以幂等键表与处理负载不膨胀。
2. 作为运维，我要被滤计数可观测，所以过滤器配置错了（全被滤）能发现。

## Implementation Decisions

- 过滤在入队前（被滤 ≠ 待投——不进 outbox/退避/死信任何路径）。
- 仅 include 语义（exclude/通配 fog 记账不预设）。

## Testing Decisions

- 命中两类型投递恰两条（被滤不投不占容量）；空集全投递回归。

## Out of Scope

- exclude 表；前缀通配；per-endpoint 订阅（多 webhook 目标）。

## Further Notes

- 与 ab.run.completed/eval.run.completed 事件族（spec 75）配合：评估事件订阅面
  零成本收窄。
