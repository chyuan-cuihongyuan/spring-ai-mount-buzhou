# Wayfinder Map — Buzhou 事件去重抑制（effort #220，B 会话第 43 轮）

> B 会话第 43 轮。at-least-once 是投递侧语义，但<b>发射侧</b>自身也可能重复
> 发同一事件（重试路径重入/多 hook 同点触发）——webhook 接收方只能全靠幂等键
> 硬扛。借鉴 CDN request coalescing 的「重复不过门」思想，在 listener 前拦。

## Destination

EventDeduplicator（core/webhook，SessionEventListener 装饰器）：onEvent 按
(type + payload sha256) 记近期指纹环（默认 1024）；重复即丢弃计数
buzhou.event.deduped；不同事件零影响透传。与 fanout 组合 = 全站入站去重。

## Notes

- 号段：B=奇数 spec（本轮 203）；轮次 .wayfinder200+。
- 与幂等键（20）互补：那是接收方防线，这是发射方防线——重复根本不出门。
- 环形有界零 TTL（指纹环滚出后同事件可再过——时间窗语义归 TTL 版留档）。

## Decisions so far

- 只拦「完全相同」（type+payload 全同）。

## Not yet specified

- TTL 时间窗去重；per-type 白名单（高敏类型永不抑制）。

## Out of scope

- 沿用各轮；语义相似去重；跨实例去重。

## Tickets

- [x] [T575 EventDeduplicator（指纹环抑制）](../tickets/T575-dedup.md)（impl-315）
- [x] [T576 去重回归（重复拦/异质过/环形滚出/透传）](../tickets/T576-dedup-tests.md)（impl-315）
