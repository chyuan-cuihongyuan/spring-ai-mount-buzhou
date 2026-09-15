# impl 1415 — HotspotRebalancer 热点重平衡建议器（R15 = effort #1814 / spec 1814 / T2829-T2830）

**What**：`HotspotRebalancer`（buzhou-spill 静态纯函数）——suggest(quantum,
tolerance, loads) 贪心搬迁建议：极差≤容差停手、单步量三重 min 封顶不越衡
反转、MAX_MOVES=10_000 保险丝、并列 id 字典序确定性；RebalancePlan 带
spreadBefore/After + improvementRatio（-1 哨兵）。

**Why**：K8s descheduler 思想——热点分片拖慢全局 P99 时「从哪搬到哪搬多少」
有建议单；容差内收手避免绝对均衡的过度搬迁（搬完倾斜回来白折腾）。

**Verify**：`HotspotRebalancerTest` 6 用例全绿（首跑编译红为 List.of 混 null
类型推断，测试侧修正）。

**Status**：done（2026-09-16）
