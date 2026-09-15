# impl 1314 — ToolCoalesceStats 合并节省读面（R15 = effort #1714 / spec 1714 / T2629-T2630）

**What**：组/参与/省去/时延四计数+savingRatio 哨兵+reset
**Why**：Go singleflight 合并回喂遥测——合并收益显形
**Verify**：ToolCoalesceStatsTest 3 断言 全绿。 **Status**：done（2026-09-15）
