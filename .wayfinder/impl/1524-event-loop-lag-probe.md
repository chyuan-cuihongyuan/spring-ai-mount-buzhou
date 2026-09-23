# impl 1524 — EventLoopLagProbe 事件循环滞后探针（R124 = effort #1923 / spec 1923 / T3047-T3048）

**What**：`EventLoopLagProbe`（core/concurrent 静态纯函数）——
lagMillis（executed−scheduled 负滞后钳 0）+ saturated 判定
（≤ threshold OK 边界含上）；时刻/阈值非负 fail-fast。

**Why**：Node.js 事件循环滞后探针惯例——任务延迟飙升被归因下游
之前，先看调度器本身还灵不灵；「调度 0ms 实际 45ms」的滞后直读
让调度器饱和前置告警。与 StealTimeReadout/PSI 互补。

**Verify**：`EventLoopLagProbeTest` 3 用例全绿（滞后钳 0/判定恰值
含上/畸形两型 fail-fast）。

**Status**：done（2026-09-23）
