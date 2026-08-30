# Wayfinder Map — Buzhou 提示前缀缓存（effort #90，A 会话）

> fog 种子⑦「前缀缓存命中率（provider 侧）」收口。轮次互斥协议同 #86 MAP。

## Destination

以提示前缀规范形为键的有界 LRU + 命中率四计数——前缀复用率可观测，「提示
工程是否在复读」有直接信号。

## Notes

- 借鉴 vLLM / SGLang radix prefix-cache；键纪律：宿主规范形 sha256，本缓存
  不猜语义相似（向量面正交）；默认 256 条 LRU（命中续命），逐出诚实计数。

## Decisions so far

- [提示前缀缓存](tickets/T453-prefix-cache.md) — get/put/getOrLoad 三面 +
  Stats(requests/hits/misses/evictions) + hitRate()（无请求 0 诚实）。

## Not yet specified

- 模型调用侧接线（ObservabilityAdvisor 前缀指纹）；缓存值 TTL/代际（系统提示
  热替换 → invalidate 之外的老化）。

## Out of scope

- 语义相似命中（向量面）；分布式前缀共享。

## Tickets

- [x] [T453 提示前缀缓存](tickets/T453-prefix-cache.md)（impl-276）
- [x] [T454 收口提交](tickets/T454-prefix-cache-close.md)（impl-276）
