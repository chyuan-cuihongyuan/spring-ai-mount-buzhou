# impl 1518 — RetryStormSentinel 重试风暴哨兵（R118 = effort #1917 / spec 1917 / T3035-T3036）

**What**：`RetryStormSentinel`（core/backpressure 静态纯函数）——
retryRatio（重试占比读数）+ isStorm（占比 ≥ 阈值判定，边界含上）；
total≥1/retried≤total/阈值∈(0,1] fail-fast。

**Why**：SRE 重试风暴惯例——故障期放大系数 Σpⁿ 让重试占比飙升，
等限流器报错时风暴已成形；占比哨兵提前一刻告警。与 RetryBudget
互补（事前预算 vs 事后告警）。

**Verify**：`RetryStormSentinelTest` 3 用例全绿（占比两例/边界含上/
畸形三型 fail-fast）。

**Status**：done（2026-09-23）
