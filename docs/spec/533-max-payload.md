# Spec 533 — webhook 载荷大小上限（effort #533）

> wayfinder map：`.wayfinder/maps/effort-533.md`（T819–T820）。E 会话第 33 轮。

## Problem Statement

outbox 有容量闸无单条体积闸——事件 payload 失控（大对象误序列化）直塞
state store 并拖垮投递/接收端。Kafka max message size 思想：生产端体积门。

## Solution

WebhookOutbox `maxPayloadChars`（默认 0 不限）：append 超限拒绝 +
`buzhou.webhook.payload-oversized` 计数；forwarder `setOutboxMaxPayloadChars`
透传；yml `buzhou.webhook.max-payload-chars`（env 直读）。

## User Stories

1. 作为运维，我想给单条 webhook 载荷设上限， so 失控大事件被拒入队
   （oversized 计数可见）而非拖垮存储与接收端。

## Implementation Decisions

- 默认不限（零默认行为变化——opt-in）。
- 拒绝而非截断（截断 JSON 事件体破坏消费端契约）。

## Testing Decisions

- 默认不限超大接受；上限内/超限边界（恰 100）；计数；setter 透传不抛。

## Out of Scope

- 压缩；分块；per-type 上限。

## Further Notes

- 无新顶层公共类型——快照零 diff 预期。
