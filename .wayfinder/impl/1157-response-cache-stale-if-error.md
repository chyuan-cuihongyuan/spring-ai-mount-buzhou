# 1157 — 响应缓存 stale-if-error

**What to build:** stale-window 宽限保留 + getStale 救场读 + advisor 失败救场 + 观测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ResponseCacheStore：staleWindow 构造 + get 宽限两态 + getStale/staleReadCount
- [x] ResponseCacheAdvisor：adviseCall 失败救场（无条目照抛）
- [x] ResilienceProperties.ResponseCache 扩参 + 兼容构造 + ResilienceModule 装配
- [x] 测试四断言全绿（resilience 367 用例）

## Done

验证：`mvn -pl buzhou-resilience test` 全绿。
