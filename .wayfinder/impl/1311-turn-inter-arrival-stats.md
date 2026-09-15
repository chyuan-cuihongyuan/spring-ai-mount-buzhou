# impl 1311 — TurnInterArrivalStats 轮间到达间隔读面（R12 = effort #1711 / spec 1711 / T2623-T2624）

**What**：静态纯函数 analyze(turnTimestamps)→InterArrivalReport(intervals/median/p95 最近秩)；<2 哨兵 −1。
**Why**：交互节奏遥测——空闲/采样调参依据。
**Verify**：TurnInterArrivalStatsTest 3 断言全绿。 **Status**：done（2026-09-15）
