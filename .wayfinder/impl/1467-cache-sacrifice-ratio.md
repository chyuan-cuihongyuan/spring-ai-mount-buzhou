# impl 1467 — CacheSacrificeRatio 缓存牺牲率（R67 = effort #1866 / spec 1866 / T2933-T2934）

**What**：`CacheSacrificeRatio`（buzhou-resilience/cache 纯 record）——
sacrificeRatio（插入驱逐比）+ hitRate + thrashing 双条件（任一哨兵无判
false）；畸形三型 fail-fast。

**Why**：体系结构缓存分析惯例思想——命中率单读数里「满缓存换血」与
「真颠簸（白忙）」同形；牺牲率×命中率双条件分诊：该扩容 vs 不动。

**Verify**：`CacheSacrificeRatioTest` 4 用例全绿（首跑红为哨兵 -1 参与
比较的实现真缺陷——修正为无据不定罪）。

**Status**：done（2026-09-16）
