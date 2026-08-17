# 171 — LRU + TTL 惰性过期

**Parent:** spec 53 §D / [T206](../tickets/T206-cache-ttl.md)

**Status:** done

- [x] 单锁 LinkedHashMap LRU（CachedEmbeddingProvider 同风格）+ 容量逐出 evicted 计数
- [x] TTL 惰性过期（命中路径检查 expireAt；过期=miss+evicted，不返回陈旧）；无后台线程
- [x] 可注入 Clock（时间行为测试零真实等待——时钟注入纪律延续）
- [x] 3 测试绿（过期/压挤序/非法配置拒绝）
