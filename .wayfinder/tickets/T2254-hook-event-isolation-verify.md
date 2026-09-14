---
id: T2254
title: HookChain 事件通知面逐 hook 异常隔离的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2253
created: 2026-09-15
---

## Question

M 会话第 2 轮：逐 hook 隔离如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=HookEventNotifyIsolationTest,HookChainTest` 全绿——
① 首位 hook onEvent 抛异常，后续 hook 仍收到同一事件；
② 裁决面 fail-fast 语义零变化（beforeTurn 抛异常仍传播——既有测试钉住，不新增放行）；
③ 抛异常调用的 onEvent 耗时仍进 stats（计时不丢账）；
④ 既有 HookChainTest 全绿零回归。
