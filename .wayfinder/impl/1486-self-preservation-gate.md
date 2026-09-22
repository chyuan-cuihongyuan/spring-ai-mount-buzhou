# impl 1486 — SelfPreservationGate 自保模式门（R86 = effort #1885 / spec 1885 / T2971-T2972）

**What**：`SelfPreservationGate`（core/concurrent 持态小门）——
onRenewal 续约计数 + resetWindow 窗口翻新（触发自保态判定）+
renewalRatio 比率读数 + shouldExpire（停逐/正常逐）+ selfPreserving
可观测面；实例数≥1/阈值∈(0,1) fail-fast，边界恰等正常逐。

**Why**：Eureka self-preservation——分区期间全部心跳同时断，朴素
剔除会把健康实例清场（重启风暴）；「大面积失联 = 自己瞎了」的
自保语义让剔除在低续约率下物理停摆、恢复自动退出。

**Verify**：`SelfPreservationGateTest` 4 用例全绿（两态/边界恰等/
计数读数/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
