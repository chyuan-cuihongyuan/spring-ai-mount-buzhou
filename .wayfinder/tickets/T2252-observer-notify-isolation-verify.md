---
id: T2252
title: SessionObserver 通知面异常隔离收口的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2251
created: 2026-09-15
---

## Question

M 会话第 1 轮：隔离收口如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=ObserverNotifyIsolationTest,GuardBlockObserverClosureTest` 全绿——
① onOpen 隔离：抛异常观察者之后注册的健康观察者仍收到 onOpen、spawn 成功；
② onTurnStart 隔离：观察者抛异常不杀轮次，chat 返回模型回复、后续观察者 start+end 双收；
③ onTurnError 隔离（流式 guard-block 通道）：订阅者仍见原始 error、后续观察者仍收 error 回调；
④ onCancel 隔离：cancel() 不抛、后续观察者仍收 onCancel；
⑤ 既有 observer 收口语义零回归（GuardBlockObserverClosureTest 不动全绿）。
