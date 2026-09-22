# impl 1493 — RampProfile 阶梯加压计划（R93 = effort #1892 / spec 1892 / T2985-T2986）

**What**：`RampProfile`（core/policy 静态纯函数 + 嵌套 Stage）——
targetAt（台阶内线性爬坡插值、末台阶后保持）+ totalDuration +
peakTarget；空表/时长 0/负目标/负时刻 fail-fast。

**Why**：k6/Gatling ramping stages——压测负载曲线先声明后执行，
采样点与负载档位一一对应；突发放满 VU 把冷启动当性能问题的根治。
与 ChaosBudgetGate 允许窗互补。

**Verify**：`RampProfileTest` 4 用例全绿（五采样点插值/边界恰值/
峰值时长声明/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
