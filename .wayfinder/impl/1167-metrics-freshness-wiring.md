# 1167 — 指标新鲜度追踪接线

**What to build:** metrics 装配链恒包 + MetricFreshnessHolder 静态 audit 面。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] BuzhouCoreAutoConfiguration 装配链包装（Micrometer → Freshness → install）
- [x] MetricFreshnessHolder（install/tracker/audit）
- [x] MetricFreshnessHolderTest 两断言 + 既有 6 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest=MetricFreshness*` 全绿。
