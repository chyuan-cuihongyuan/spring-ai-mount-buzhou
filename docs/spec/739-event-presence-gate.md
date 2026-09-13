# 739 — 事件静默缺失门

> 来源：G 会话第 40 轮 = effort #739（726 分布的对偶面）/ [T1078](../../.wayfinder/tickets/T1078-event-presence-gate.md) / [T1079](../../.wayfinder/tickets/T1079-event-presence-gate-verify.md) / impl 639。

## Problem

726 分布回答「什么在发生」——但生命周期契约是「什么**该**发生」：会话必有 started/finished、turn 必有 completed……该发生而从未出现（缺失）是流程断链的前向信号，散落在「没有证据」里最难发现。

## Solution

`EventTypePresenceGate.gate(events, expectedTypes)` 纯函数：observed 类型集与期望集差集 → missing（字典序）+expectedCount/observedTypes。空契约不误报（无契约=不检查）。纯读数，门禁动作（拦截/告警）归消费方。

## Out of Scope

次数下限（≥N 次——本面只管有无）；契约声明面（宿主按机制自定）。
