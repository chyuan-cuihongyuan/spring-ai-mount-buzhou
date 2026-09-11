# 489 — 衰减过滤可观测

**What to build:** DecayingFactStore.filteredCount()（过滤分支内 AtomicLong）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] getter + 计数断言
- [x] core/guard 全模块零回归
- [x] spec 636 + README 行

## Done

验证：`mvn -pl buzhou-core,buzhou-guard -am test` 绿。commit 见本轮 `feat(core)` 提交。
