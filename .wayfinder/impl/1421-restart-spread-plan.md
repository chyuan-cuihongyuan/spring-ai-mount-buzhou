# impl 1421 — RestartSpreadPlan 重启错峰计划（R21 = effort #1820 / spec 1820 / T2841-T2842）

**What**：`RestartSpreadPlan`（buzhou-resilience 静态纯函数）——delayFor
稳定哈希分槽（|id.hashCode| mod cohortSize → slot×window/cohortSize ∈
[0,window)）+ cohort 普查（delays/maxDelay/collisions + collisionRatio
-1 哨兵）；空白 id/cohortSize<1/窗<1 fail-fast。

**Why**：memberlist/consul 协同重启错峰 + AWS jitter 思想——同批同时重试是
重启风暴（打爆下游+集体退避共振）；确定性哈希槽（vs 随机 jitter）可回放
可审计，碰撞风险面诚实入账。

**Verify**：`RestartSpreadPlanTest` 5 用例全绿。

**Status**：done（2026-09-16）
