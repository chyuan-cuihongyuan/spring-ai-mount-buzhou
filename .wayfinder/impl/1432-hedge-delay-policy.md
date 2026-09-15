# impl 1432 — HedgeDelayPolicy 对冲延迟策略（R32 = effort #1831 / spec 1831 / T2863-T2864）

**What**：`HedgeDelayPolicy`（buzhou-resilience 静态纯函数）——
hedgeThresholdMillis（最近秩分位/不足退守地板）+ decide（边界含上）；
默认 P95/10ms/20 常量；畸形五型 fail-fast（样本校验先于退守——初版
漏检自查修正）。

**Why**：Google Tail at Scale hedged requests 思想——立即双发是双倍负载、
固定延迟是魔法数；P95 后才对冲让尾部 5% 才付双倍钱、中位数零成本、
冷启动退守不冒进。

**Verify**：`HedgeDelayPolicyTest` 4 用例全绿。

**Status**：done（2026-09-16）
