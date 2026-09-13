# 782 — 模型预算闸判定分布读面

**What to build:** ModelBudgetGate checks/allowed/blocked 三计数 + 嵌套 BudgetGateStats + stats() + 记账驱动耗尽测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数埋点（beforeModel 判定单点）
- [x] BudgetGateStats 嵌套 record + stats()
- [x] BudgetGateStatsTest（预算内放行/越限拦截/守恒/未声明预算恒放行）
- [x] spec 1029 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='BudgetGateStatsTest,ModelBudgetGateTest'` 全绿。commit 见本轮 `feat(core)` 提交。
