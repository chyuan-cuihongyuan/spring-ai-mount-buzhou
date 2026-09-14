# 1056 — EWMA 自适应超时推荐器

**What to build:** AdaptiveTimeout 纯推导器（EWMA α=0.3 + clamp(EWMA×3, floor, ceiling) + 预热哨兵 + stats()/resetForTest()）+ 七测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] AdaptiveTimeout（resilience 根包，CAS 无锁，fail-fast 参数校验）
- [x] AdaptiveTimeoutTest（EWMA 数学/预热/夹取/恢复/reset/fail-fast/无副作用快照）
- [x] spec 1403 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-resilience -am test -Dtest='AdaptiveTimeoutTest'` 7/7 绿。
