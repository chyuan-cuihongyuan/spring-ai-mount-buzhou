# Wayfinder Map — Buzhou webhook 订阅类型过滤（effort #67，50 轮自迭代第 32 轮）

> effort #67，延续 #66（T385–T386 / impl-251）。主线：webhook 全事件投递——
> 消费端只要 user.turn.completed 一类也被灌入 tool.called 等全部事件（outbox 容量
> 与消费端幂等负担双吃）。GitHub/Stripe webhook 的事件订阅面是成熟形态。

## Destination

`WebhookEventForwarder.setIncludeTypes(Collection<String>)`：空集 = 全投递（默认零
变化）；命中才入队（被滤事件不占 outbox 容量——不是待投事件）+ 计数器
`buzhou.webhook.filtered`；装配经 Binder 读 `buzhou.webhook.include-types`
（List<String>）；BuzhouWebhookProperties record 不扩（构造链兼容——独立注入面）。

## Notes

- 借鉴：GitHub/Stripe webhook 事件订阅（消费端只收关心的类型）。

## Decisions so far

- 过滤在入队前（被滤事件不进 outbox——容量语义只属于待投事件）。
- include 语义（无 exclude——最小面；排除表需求证据后议）。

## Not yet specified

- exclude 排除表；前缀通配（user.turn.*）。

## Out of scope

- 沿用 #7–#66。

## Tickets

- [x] [T389 includeTypes 过滤 + 装配接线](tickets/T389-webhook-filter.md)（impl-252）
- [x] [T390 2 例红队（命中才投/空集全投）+ forwarder 回归 + 收口](tickets/T390-webhook-filter-close.md)
