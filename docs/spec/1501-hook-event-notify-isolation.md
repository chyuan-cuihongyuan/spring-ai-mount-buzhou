# 1501 — HookChain 事件通知面逐 hook 异常隔离

> 来源：M 会话第 2 轮 = effort #1501（impl 1104）。spec 1500（Guava EventBus SubscriberExceptionHandler）在 hook 事件通知域的同源应用。

## 背景

`DefaultAgentSession.deliverEvent` 对整条 `hookChain.fireEvent` 已有链级 try/catch（单链异常不炸事件分发与 close 清理），但链内无隔离：第 i 个 hook 的 `onEvent` 抛 RuntimeException 会跳过第 i+1..n 个 hook 的事件通知——观测/记录类 hook 静默丢事件，且无日志定位。

## 目标

- 通知面与裁决面分离：`run()`（Block/Replace 裁决面）保持 fail-fast 治理语义不变；`fireEvent`（通知面：返回 void、无裁决语义）改为链内逐 hook try/catch RuntimeException → ERROR 日志（hook 名 + 事件类型 + 栈）后继续其余 hook；
- 计时仍记录（try/finally——失败调用的耗时也入 per-hook stats）。

## 兼容性

行为修复型变化：hook 事件通知从「链内第一个异常吞掉其余 hook」改为「逐 hook 隔离」。无新增公共 API；裁决面（before/afterTurn 等）抛异常传播语义零变化。
