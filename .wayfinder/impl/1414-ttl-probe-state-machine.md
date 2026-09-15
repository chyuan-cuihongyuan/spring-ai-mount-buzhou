# impl 1414 — TtlProbeStateMachine TTL 探针状态机（R14 = effort #1813 / spec 1813 / T2827-T2828）

**What**：`TtlProbeStateMachine`（core/health 静态纯函数）——evaluate 三态
（PASSING/STALE/CRITICAL，双边界含上）+ freshness 剩余新鲜度（到期钳 0）+
census 三态普查（criticalRatio -1 哨兵）；负年龄/TTL<1/warnFraction 越界或
NaN/空 id fail-fast。

**Why**：Consul health check TTL 思想——到期靠时间自然到期判定（零轮询），
STALE 预警线给「续命在即」先兆、freshness 给可运营梯度，二值健康升级为
三态梯度。

**Verify**：`TtlProbeStateMachineTest` 4 用例全绿。

**Status**：done（2026-09-16）
