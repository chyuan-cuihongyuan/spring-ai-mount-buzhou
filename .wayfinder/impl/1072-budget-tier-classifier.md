# 1072 — 预算分档分类器

**What to build:** BudgetTierClassifier 纯函数（classify→TierReport 三档+UNKNOWN+四桶计数+tightest）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] BudgetTierClassifier（core/budget，private 构造静态面）
- [x] BudgetTierClassifierTest 五测（边界/四桶/tightest/畸形/越限）
- [x] spec 1419 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='BudgetTierClassifierTest'` 5/5 绿。
