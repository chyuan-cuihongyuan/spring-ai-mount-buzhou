# 464 — 语义缓存维度漂移可见性

**What to build:** SemanticCacheStore 维度不匹配计数 + 首次 WARN + `dimensionMismatches()` 观测面。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 维度检查前移 + 计数 + WARN 去重
- [x] 3 用例绿（计数/不受影响/自然收敛）
- [x] spec 611 + README 行

## Done

验证：`mvn -pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(resilience)` 提交。
