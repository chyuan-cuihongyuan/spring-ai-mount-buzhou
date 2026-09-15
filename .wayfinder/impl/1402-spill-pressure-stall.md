# impl 1402 — SpillPressureStall PSI 失速读面（R2 = effort #1801 / spec 1801 / T2803-T2804）

**What**：`SpillPressureStall`（buzhou-spill 静态纯函数）——`StallSample
(active, stalled)` 单窗事实 + `analyze` → PsiReport 双档账目（some=≥1 失速、
full=活跃全失速；空闲窗只进分母）+ somePct/fullPct/worstStallRatio 读数
（空观测 -1 哨兵）。

**Why**：Linux PSI 思想——压力不看水位看「谁在等」：somePct 高=局部会话付
延迟税（加阈值余量），fullPct 高=整个运行时等磁盘（换更快存储）——两档分開
回答不同处方。

**Verify**：`SpillPressureStallTest` 5 用例全绿（分档账目/空闲窗/哨兵/
fail-fast/并列峰值取宽窗）。

**Status**：done（2026-09-16）
