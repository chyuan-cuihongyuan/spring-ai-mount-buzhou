# impl 1517 — ApiSunsetLifecycle API 弃用日落生命周期（R117 = effort #1916 / spec 1916 / T3033-T3034）

**What**：`ApiSunsetLifecycle`（core/policy 静态纯函数 + Phase 枚举）
——phase 三段判定（ACTIVE/DEPRECATED/SUNSET 边界含上）+
daysRemainingMillis 剩余读数钳 0；弃用线 ≤ 日落线 fail-fast。

**Why**：Stripe/GitHub 版本日落语义——弃用只有删与不删两态太粗；
三段生命周期让调用方有倒计时、宿主有删除依据。与 ToolDeprecation
配置面互补（声明存储 vs 时刻判定）。

**Verify**：`ApiSunsetLifecycleTest` 4 用例全绿（三段/边界含上/
剩余钳 0/畸形两型 fail-fast）。

**Status**：done（2026-09-23）
