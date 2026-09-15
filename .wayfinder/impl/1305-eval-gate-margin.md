# impl 1305 — EvalGateMargin 门限边际直方（R6 = effort #1705 / spec 1705 / T2611-T2612）

**What**：`EvalGateMargin`（core/eval 静态纯函数）——`analyze(passRates,
threshold)` → `MarginReport(runs/threshold/margins/min/max)` + `withinBand(band)`
危险带计数；两侧对称；空表哨兵 −1。

**Why**：「过 0.003」与「过 0.2」同是过含金量天壤——边际分布回答门置信底座
厚度（Google SRE 告警边际/SPRT 边际思想）。

**Verify**：`EvalGateMarginTest` 3 断言（边际账目+带内三档/空哨兵/两侧同权+
null）——浮点容差断言（within 1e-9，假红修正）。

**Status**：done（2026-09-15）
