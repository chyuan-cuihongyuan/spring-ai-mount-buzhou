---
Type: task
Status: closed
---
## Question

Redis 共享语义向量缓存：桶 hash 键 + 客户端 cosine 最近邻 + 跨实例共享。

## Resolution

done（2026-08-30）：impl-275；RedisSemanticVectorCache（Lettuce HASH、LRU-by-seq
容量驱逐、TTL 双层惰性过期、hit/miss/bypass 计数；Redis 不可达旁路 miss 不 fail-fast）。
