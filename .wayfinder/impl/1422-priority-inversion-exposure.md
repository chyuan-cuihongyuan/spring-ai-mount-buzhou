# impl 1422 — PriorityInversionExposure 优先级反转暴露（R22 = effort #1821 / spec 1821 / T2843-T2844）

**What**：`PriorityInversionExposure`（core/concurrent 静态纯函数）——
HeldResource/Waiter 事实（rank 值小=关键）+ analyze → Exposure（inversions/
worstRankGap/unknownResourceWaiters + inversionRatio(-1 哨兵)）；空白 id
fail-fast；null 任一按空表。

**Why**：OS 优先级反转（Mars Pathfinder 教训）思想——低优持有者挡高优
等待者，关键路径被无关负载拖死而系统看似无故障；暴露读数把「关键轮次卡
在批量杂活持有的资源上」变成可计数风险面。

**Verify**：`PriorityInversionExposureTest` 4 用例全绿。

**Status**：done（2026-09-16）
