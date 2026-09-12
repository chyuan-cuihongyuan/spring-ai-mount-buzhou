# Wayfinder Map — Buzhou webhook 载荷大小上限（effort #533，E 会话第 33 轮）

> E 会话第 33 轮。勘察：outbox 容量闸（满拒入队）有——**单条载荷体积**
> 无上限：事件 payload 失控（大对象误序列化）直塞 state store + 投递
> 拖垮接收端。Kafka max message size 思想。

## Destination

WebhookOutbox maxPayloadChars（setter 包级——forwarder 透传）：append
超限拒绝 + buzhou.webhook.payload-oversized 计数；默认 0 不限（零默认
行为变化 opt-in）。装配：buzhou.webhook.max-payload-chars（env 直读）
→ forwarder.setOutboxMaxPayloadChars。

## Notes

- 号段：spec 533 / T819–820 / impl-435。
- 借鉴源：Kafka max message size / nginx client_max_body_size。

## Out of scope

- 载荷压缩；分块投递；per-type 上限。

## Tickets

- [x] [T819 outbox 载荷闸](../tickets/T819-outbox-max-payload.md)
- [x] [T820 forwarder 透传](../tickets/T820-max-payload-forwarder.md)
