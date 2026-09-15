# impl 1320 — DryRunDecisionStats dry-run 决策分布（R21 = effort #1720 / spec 1720 / T2641-T2642）

**What**：Decision 三态闭集（WOULD_RUN/WOULD_BLOCK/PLAN_ERROR）+census+blockRatio（无样本 −1）+resetForTest。
**Why**：Terraform plan 决策分布——dry-run 价值量度（拦了多少）。
**Verify**：DryRunDecisionStatsTest 3 断言全绿。 **Status**：done（2026-09-15）
