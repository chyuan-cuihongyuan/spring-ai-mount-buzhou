---
Type: task
Status: closed
---
## Question

工具结果跨轮 TTL 复用窗：命中/过期/逐出/失败不缓存。

## Resolution

done（2026-08-30）：impl-305；TtlCachingToolCallback（Clock 注入 + LRU 封顶
+ hit/miss/evicted 计数 + argsHash 键）。
