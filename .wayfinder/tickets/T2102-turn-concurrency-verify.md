---
id: T2102
title: TurnConcurrencyTracker 守恒式与 guard-block 收口的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2101
created: 2026-09-14
---

## Question

如何证明守恒式、峰值水位单调性与缺陷修复的端到端有效性？

## Resolution

**用户常设授权 AFK（可推翻）**

三层验证全绿（`mvn -pl buzhou-core -am test`）：
1. `TurnConcurrencyTrackerTest` 五测：ok/failed 混合守恒、峰值单调不回退、双终结防御 active 不为负、resetForTest 全零、8 线程×500 轮并发压测最终守恒（G r47 压测模式）。
2. `GuardBlockObserverClosureTest` 三测（E2E）：非流式 guard-block 补派 onTurnEnd（reason 即 finalReply、模型未触达、护栏返回语义不变）；流式 guard-block 补派 onTurnError（与订阅者 ISE 对称）；tracker 守恒式端到端闭合。
3. 全模块回归 2357 测仅 1 flaky（HookEndToEndTest 满载 60s 竞态，单独重跑 1.5s 绿——与 diff 零交集，已知 flaky 族）。
