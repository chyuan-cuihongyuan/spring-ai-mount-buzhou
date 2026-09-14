---
id: T2101
title: 跨会话轮次并发水位观察者（TurnConcurrencyTracker）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 1 轮：应用级轮次并发读面的形状与挂点选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：会话内轮次单飞（spec 40 §B CAS 0→1），`inFlightTurns()` 仅 per-session 点读数（runtime 排空裁决源）；跨会话聚合/峰值/生命周期总量全无。Hook seam 的 afterModel 流式路径逐 chunk 派发——并发追踪挂 Hook 会漏账；**SessionObserver seam 恰好 once-per-turn**（finalized CAS 守卫）且带 onTurnError——为正解挂点。

形状裁决：`TurnConcurrencyTracker implements SessionObserver`（core/session 实例面）——同实例注册全部会话即应用级聚合；started/okFinished/failed 三总量 + active AtomicInteger + peakActive accumulateAndGet 单调水位；守恒式 `started = okFinished + failed + active`；stats()/resetForTest()。**同轮实证缺陷**：两处 guard-block 路径派发 onTurnStart 后无终结回调（TURN span 泄漏）——非流式补 onTurnEnd（reason 即最终回复）、流式补 onTurnError（与订阅者 error 对称），护栏返回语义逐位不变。

Out of scope：per-session 峰值（inFlightTurns 已有点读）；模型调用级并发（归 TurnTimingHook 族）；outcome=blocked 计时档。
