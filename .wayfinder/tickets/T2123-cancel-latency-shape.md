---
id: T2123
title: 取消延迟追踪读面（CancelLatencyTracker）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 12 轮：取消「信号→实际停止」时延面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：cancel() 即返（requestCancel+事件+指标），实际终结异步（流式 doFinally→failTurnOnce→onTurnError(CancellationException)）；无时延读面。H 824 是原因分布（为何取消），本轴是时延分布（多久真停）——正交。

形状裁决：CancelLatencyTracker implements SessionObserver（opt-in）——onTurnStart 标记在途/onCancel 仅在途入键/onTurnEnd·onTurnError 消费未决取消入环清键（单飞无并发轮竞态）；环 64+recent-rank P50/P95+pendingCancels；非流式无外部取消缝如实入档 Out of Scope。

Out of scope：非流式时延；超时判定（TurnStallWatchdog 域）；cause 分桶。
