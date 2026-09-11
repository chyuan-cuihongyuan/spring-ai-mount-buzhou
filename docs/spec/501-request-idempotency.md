# Spec 501 — 请求幂等键（effort #501）

> wayfinder map：`.wayfinder/maps/effort-501.md`（T753–T754）。E 会话第 2 轮。

## Problem Statement

响应缓存（spec 53）按内容哈希键命中同问法；客户端**超时重试**场景下
重试请求与原请求语义相同但字节不同（内嵌时间戳/随机数），内容缓存必
miss → 二次真调二次计费二次副作用。调用方供给的幂等键（Stripe
Idempotency-Key 语义）空白：同键重入应重放首次结果。

## Solution

`resilience.idempotency.IdempotencyAdvisor` + `BuzhouIdempotencyProperties`
（Stripe 借鉴；存储复用 ResponseCacheStore）：

- **Advisor**（BaseAdvisor，链序 +440——response-cache(+450) 外）：
  advisor 参数 `buzhou.idempotency-key`（公开常量）读取；缺席=透传零
  行为。命中 → 重放缓存响应（新建包装防共享可变引用）+ replayed 计数；
  未命中 → nextCall，终态（复用 `ResponseCacheAdvisor.isTerminal`——无
  toolCalls 且内容非空）才写 + stored 计数。流式：命中单元素重放；未
  命中聚合完整后写、取消/错误不写半截（53 流式组装同法）。
- **存储**：复用 `ResponseCacheStore`（LRU+TTL 惰性过期+hit/miss/evict
  计数自带——同族语义不为第二用途造第二存储）；键加 "idem:" 前缀内部
  命名空间防与内容缓存混用。
- **yml**：`buzhou.resilience.idempotency.{enabled, ttl, maxEntries}`——
  enabled 默认 false（opt-in）；ttl 默认 24h（Stripe 同款）；maxEntries
  默认 1024。Binder 预绑条件装配（426 同法）。

## User Stories

1. 作为宿主，我想为每轮对话供给幂等键，so 客户端超时重试不会二次调用
   模型二次计费（同键重放首次响应）。
2. 作为运维，我想幂等重放可观测，so 重试风暴（replayed 激增）可见。

## Implementation Decisions

- 链序 +440（cache 外）：同键重入跳过全部下游含内容缓存读——重放是
  「这是同一次调用」的最强声明。
- 不校验 payload 与键首见一致（宿主保证同键同请求——Stripe 同注记，
  指纹校验留 Out of Scope）。
- 单实例存储诚实边界（跨实例共享属已否决 Redis 缓存族）。

## Testing Decisions

- 同键两次 call——第二次零链调用（StubChain 消费计数=1）且文本同；
  不同键两次真调；无键透传每次真调。
- 非终态（空内容）不写——同键第二次仍真调。
- 流式同键第二次=单元素重放聚合文本；流命中/存储计数经 store 观测。
- yml：enabled=true 装配 bean、缺省无 bean。

## Out of Scope

- 跨实例幂等键；payload 指纹；键列举/撤销管理面。

## Further Notes

- 新公共类型 `IdempotencyAdvisor`、`BuzhouIdempotencyProperties` 随轮
  regenerate 快照 + api-surface.md 加行。
