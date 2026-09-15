# impl 1403 — TurnDeadlineBudget 轮墙钟预算传播（R3 = effort #1802 / spec 1802 / T2805-T2806）

**What**：`TurnDeadlineBudget`（core/exec 静态纯函数）——`plan(totalNanos,
estimates)` 逐调用裁决：剩余预算顺序递减、获准超时=min(预估,剩余)（截断）、
剩余=0 拒绝（零耗也不例外——deadline 先于派发检查）；BudgetPlan 带
committedNanos/admissionRatio（空计划 -1 哨兵）。

**Why**：gRPC deadline propagation / Temporal schedule-to-close 思想——预算沿
调用链传播而非各自独立计时：轮墙钟不被 N 次单超时拖成 N 倍，快耗尽时新调用
不起工省掉注定被砍的半成品工。

**Verify**：`TurnDeadlineBudgetTest` 5 用例全绿（截断/前缀性/零预算/哨兵/
fail-fast）。

**Status**：done（2026-09-16）
