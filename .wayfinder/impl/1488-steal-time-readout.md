# impl 1488 — StealTimeReadout 窃取时间读面（R88 = effort #1887 / spec 1887 / T2975-T2976）

**What**：`StealTimeReadout`（core/metrics 静态纯函数）——
stealRatio（Δsteal/Δtotal 两采样差分，Δtotal=0 哨兵 0.0）+
isContended（阈值判定）；计数单调/阈值 ∈[0,1] fail-fast。

**Why**：Linux steal time——宿主超卖下「CPU 不忙但慢」的唯一可见
信号（idle 看似充足实为 tick 被偷）；占比读数让容量排查不再死胡同。
与 PSI 读面（#1801）互补。

**Verify**：`StealTimeReadoutTest` 4 用例全绿（经典 6%/阈值两侧恰
等/零流逝哨兵/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
