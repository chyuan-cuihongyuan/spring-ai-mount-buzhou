# impl 1302 — EvalCoverageMatrix 评测集覆盖矩阵（R3 = effort #1702 / spec 1702 / T2605-T2606）

**What**：`EvalCoverageMatrix`（core/eval 静态纯函数）——`build(itemLabelSets)` →
`CoverageReport(itemCount/labelCounts/distinctLabels)`；`missingFrom(universe)`
漏测清单；`shannonEntropy()` 归一化香农熵 0..1（ln k 归一，k≤1 记 0）。

**Why**：「测了什么」先于「测得怎样」——标签分布偏科用熵显形（JaCoCo/Stryker
覆盖矩阵 + scikit-learn 信息熵思想）。

**Verify**：`EvalCoverageMatrixTest` 4 断言（计数去重/差集字典序/熵契约/
null 与无标签项口径）。

**Status**：done（2026-09-15）
