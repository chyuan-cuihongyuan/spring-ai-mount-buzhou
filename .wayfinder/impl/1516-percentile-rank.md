# impl 1516 — PercentileRank 百分位排位（R116 = effort #1915 / spec 1915 / T3031-T3032）

**What**：`PercentileRank`（core/eval 静态纯函数）——rank（≤ value
样本占比，极值钳 0.0/1.0）+ percentileOf（×100 报表直读）；samples
非空非负 fail-fast。

**Why**：统计学 percentile rank——「这次 12s 算慢吗」用绝对阈值
跨场景失配；与历史样本比的相对排位是同一把尺。与 PSquareQuantile
互补（按占比取值 vs 按值给排位）。

**Verify**：`PercentileRankTest` 4 用例全绿（排位 0.8/极值钳位/
百分位直读 80/畸形两型 fail-fast）。

**Status**：done（2026-09-23）
