# impl 1306 — ForkShapeStats fork 树形态普查（R7 = effort #1706 / spec 1706 / T2613-T2614）

**What**：静态纯函数 analyze(childToParent)→ShapeReport 六读数；环路拒绝。
**Why**：fork 深链/扇出热点/叶子面治理显形（git DAG 思想）。
**Verify**：ForkShapeStatsTest 5 断言全绿。 **Status**：done（2026-09-15）
