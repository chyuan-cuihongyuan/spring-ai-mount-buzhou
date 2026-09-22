# impl 1489 — PoolSizeHeuristic 连接池容量启发（R89 = effort #1888 / spec 1888 / T2977-T2978）

**What**：`PoolSizeHeuristic`（core/concurrent 静态纯函数）——
optimalSize（cores×2+spindles）+ splitBudget（预算均衡拆分余数摊
前）+ saturationRatio（饱和度读数零池哨兵）；cores≥1/spindles≥0/
预算≥节点数 fail-fast。

**Why**：HikariCP wiki 反直觉结论——连接数超过线程可驱动数只会更
慢；单节点公式 + 多节点预算拆分 + 饱和度读数让部署期容量基线有据。
与 GradientAdaptiveLimiter（运行时自适应）互补。

**Verify**：`PoolSizeHeuristicTest` 5 用例全绿（经典 10/SSD 8/拆分
{6,6,6,5}/饱和 0.95 哨兵/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
