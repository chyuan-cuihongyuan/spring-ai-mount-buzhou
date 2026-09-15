# impl 1431 — MultiLevelCacheStats 多级缓存读面（R31 = effort #1830 / spec 1830 / T2861-T2862）

**What**：`MultiLevelCacheStats`（buzhou-resilience/cache 静态纯函数）——
CacheReport 四路合计契约 + l1HitRate/combinedHitRate/l1DerelictionRate
（失职率分母=非回源，哨兵 -1）。

**Why**：Caffeine multi-level/CPU L1-L2 思想——合并命中率让「L1 形同虚设」
与「L2 无效」同形；逐层命中率+失职率三分诊：升容量/查预热/查键口径。

**Verify**：`MultiLevelCacheStatsTest` 4 用例全绿。

**Status**：done（2026-09-16）
