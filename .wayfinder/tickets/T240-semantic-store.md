---
Type: task
Status: done
---
## Question

SemanticCacheStore（resilience.cache）：向量条目（embedding float[] + 终态 ChatResponse
+ expireAt + 桶键）；分桶 = modelName + options 采样（与精确缓存 ResponseCacheKeys 同口径）；
桶内线性 cosine 相似度 ≥ 阈值（默认 0.95 可配）取最近邻；LRU + TTL 惰性过期（复用
ResponseCacheStore 风格）；可注入 Clock；hit/miss/evicted 计数可读。
