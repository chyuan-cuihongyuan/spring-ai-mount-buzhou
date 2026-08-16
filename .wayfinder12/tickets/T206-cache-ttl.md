---
Type: task
Status: closed
blocked-by: T203-cache-advisor-key.md
---
## Question

LRU 容量（默认 256）+ TTL 惰性过期（默认 1h；读取时检查，无需后台线程）；过期/容量逐出
evicted 计数；CachedEmbeddingProvider 同风格。

## Resolution

impl-171 落地：LRU+TTL 惰性过期（可注入 Clock）；过期=miss+evicted 不返回陈旧；非法配置拒绝。
3 测试绿。T206 关闭。
