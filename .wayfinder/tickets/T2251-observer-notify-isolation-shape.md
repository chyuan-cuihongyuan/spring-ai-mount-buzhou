---
id: T2251
title: SessionObserver 通知面异常隔离收口的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 1 轮（开张轮 + 首个实质主题）：DefaultAgentSession 的 SessionObserver 通知点异常隔离口径？

## Resolution

**用户常设授权 AFK（可推翻）**

现状实证：close() 的 onClose（失败收集）与 deliverEvent 的 hook 链/逐 listener 均已隔离（impl-30 / spec 13 §core-1 先例），但 observer 其余 12 处通知点为裸 forEach——onOpen（构造器尾部）/onTurnStart×2/onTurnEnd×3/onTurnError×5/onCancel。单个观察者抛 RuntimeException 即：中断其余观察者通知、向上传播破坏轮次（非流式直接失败/流式订阅 error）、onOpen 场景炸掉会话构造且半初始化资源（leakHandle 已 track 无 close）泄漏。

形状：新增私有 `notifyObservers(Consumer<SessionObserver>)` 隔离派发（逐个 try/catch RuntimeException → ERROR 日志带 sessionId，不中断不传播），12 处统一改走；onClose 既有「失败收集 + suppressed 聚合上抛」语义不动（close 是终结路径，需可观测失败）。思想源：Guava EventBus SubscriberExceptionHandler（单订阅者异常不影响其余订阅者与主流程）。语义变化即修复：观测组件缺陷不再放大为业务故障——与既有隔离决策（impl-30）同向。
