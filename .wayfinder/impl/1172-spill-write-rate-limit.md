# 1172 — spill 写速率限速

**What to build:** SpillWriteRateLimiter + DiskSpillStore 节流接线。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SpillWriteRateLimiter（令牌桶 + 软限速 + 观测计数）
- [x] DiskSpillStore.store 写盘前节流（中断放行语义）
- [x] 五断言 + spill 180 用例零回归
- [x] 就地修复邻居半成品（metaPath 未定义引用）

## Done

验证：`mvn -pl buzhou-spill test` 全绿。
