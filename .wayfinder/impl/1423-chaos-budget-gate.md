# impl 1423 — ChaosBudgetGate 混沌预算门（R23 = effort #1822 / spec 1822 / T2845-T2846）

**What**：`ChaosBudgetGate`（core/exec 静态纯函数）——Window 契约构造 +
decide 三态（窗口优先于预算）+ usage 使用账（钳零/烧尽比/超支照实）；
负预算/倒挂窗/null 与负花费 fail-fast。

**Why**：Netflix Chaos Monkey/Chaos Toolkit 思想——无界混沌等于自己 DDoS
自己；低峰窗+周期预算闸住爆炸半径，与 ChaosMonkeyHook 执行器成对。

**Verify**：`ChaosBudgetGateTest` 4 用例全绿。

**Status**：done（2026-09-16）
