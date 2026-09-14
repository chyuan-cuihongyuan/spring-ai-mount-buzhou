# 1153 — 语义缓存 LFU 采样驱逐

**What to build:** `SemanticCacheStore` 驱逐升级——采样窗口内最低命中数先出（opt-in，默认纯 LRU 零变化），
配置面 + 装配 + hotPreservedCount 观测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] store：evictionSampleSize 参数 + CacheEntry 命中计数 + evictOne 统一驱逐（容量/权重两路径）
- [x] 观测：hotPreservedCount（采样生效保护热条目次数）
- [x] 配置：semantic-cache.eviction-sample-size（默认 0=关，fail-fast 校验）
- [x] 装配：ResilienceModule 传参
- [x] 测试：SemanticCacheLfuEvictionTest 五断言全绿

## Done

验证：`mvn -pl buzhou-resilience test`（resilience 模块全量）绿；行为断言见 spec 1600 Testing Decisions。
