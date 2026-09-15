# impl 1342 — RangeLocalityStats 范围读局部性分类读面（R43 = effort #1742 / spec 1742 / T2685-T2686）

**What**：连续性分类（顺序/随机/首读独立）+顺序占比哨兵
**Why**：RocksDB 局部性——预取与碎片化诊断
**Verify**：RangeLocalityStatsTest 3 断言 全绿。 **Status**：done（2026-09-15）
