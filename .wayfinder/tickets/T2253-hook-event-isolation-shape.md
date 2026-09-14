---
id: T2253
title: HookChain 事件通知面逐 hook 异常隔离的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 2 轮：HookChain.fireEvent 链内单 hook onEvent 抛异常的处置口径？

## Resolution

**用户常设授权 AFK（可推翻）**

现状实证：DefaultAgentSession.deliverEvent 对整条 hookChain.fireEvent 做了链级 try/catch（单链异常不炸事件分发与 close 清理），但链内无隔离——第 i 个 hook 的 onEvent 抛 RuntimeException 会跳过第 i+1..n 个 hook 的事件通知（观测/记录类 hook 静默丢事件）。

形状：通知面与裁决面分离——run()（before/afterTurn 等，有 Block/Replace 裁决语义）保持 fail-fast 治理语义不动；fireEvent（通知面：返回 void、无裁决、hook 只读消费事件）改为链内逐 hook try/catch RuntimeException → ERROR 日志（hook 名 + 事件类型 + 栈），继续其余 hook，计时仍记录（try/finally，失败调用的耗时也入账）。思想源：spec 1500（Guava EventBus SubscriberExceptionHandler）在 hook 事件通知域的同源应用——观测组件缺陷不放大为通知面残缺。
