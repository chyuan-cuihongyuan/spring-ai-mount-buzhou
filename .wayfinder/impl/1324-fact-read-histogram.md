# impl 1324 — FactReadHistogram 事实读热分桶（R25 = effort #1724 / spec 1724 / T2649-T2650）

**What**：逐键计数有界 512+四档（冷温热灼）census+匿名桶
**Why**：Redis LFU 频次直方——缓存与衰减调参依据
**Verify**：FactReadHistogramTest 3 断言 全绿。 **Status**：done（2026-09-15）
