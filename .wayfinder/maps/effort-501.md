# Wayfinder Map — Buzhou 请求幂等键（effort #501，E 会话第 2 轮）

> E 会话第 2 轮（原主题池 R2=提示词注入特征扫描——勘察撞已有能力弃：
> guard.classifier.InjectionClassifier 概率分类器 + OnnxPromptGuard +
> Builder.injectionDefense() 已是纵深一层；SsrfGuard 也已覆盖 HTTP 出口
> 白名单族）。换题：请求幂等键（Stripe Idempotency-Key）。
> 勘察：响应缓存（53）按**内容哈希**键——同问法命中；但客户端超时重试
> 场景（同键≠同字节 payload，如内嵌时间戳）会二次真调二次计费。调用方
> 供给的**幂等键**语义空白：同键重入=重放首次结果不二次烧钱。

## Destination

`resilience.idempotency.IdempotencyAdvisor`（BaseAdvisor，链序 +440——
response-cache(+450) 外：同键重入连缓存读都跳过；advisor 参数
`buzhou.idempotency-key` 读取（request.context().get——BuzhouMemoryAdvisor
同 API），缺席=透传零行为）+ 存储复用 `ResponseCacheStore`（LRU+TTL
ChatResponse 存储，hit/miss/eviction 计数自带——不为同族语义造第二存储）；
写边界复用 `ResponseCacheAdvisor.isTerminal` 公共判定（无 toolCalls 且
内容非空才存——非终态不写半截）。计数 `buzhou.resilience.idempotency.
{replayed,stored}`。`BuzhouIdempotencyProperties`（enabled 默认 false/
ttl 默认 24h=Stripe 同款/maxEntries 默认 1024）+ Binder 预绑条件装配
（426 三 bean 同法：store+RuntimeConfig(assemblyCustomizer)）。

## Notes

- 号段：spec 501 / T753–T754 / impl-404。
- 借鉴源：Stripe Idempotency-Key（同键 24h 重放首次响应）。
- 诚实边界：①单实例 LRU（跨实例共享属已否决 Redis 缓存族）；②同键重放
  不校验 payload 一致（宿主保证同键同请求语义——Stripe 同注记）；
  ③重放响应与首次共享不可变 ChatResponse（新建包装——53 命中同法）。
- 键前缀 "idem:" 内部命名空间防与内容缓存键混用。

## Out of scope

- 跨实例幂等键存储（Redis 域，已否决族）；payload 指纹校验；键管理面
  （列举/撤销）。

## Tickets

- [x] [T753 IdempotencyAdvisor 重放语义](../tickets/T753-idempotency-advisor.md)
- [x] [T754 幂等键 yml 装配](../tickets/T754-idempotency-assembly.md)
