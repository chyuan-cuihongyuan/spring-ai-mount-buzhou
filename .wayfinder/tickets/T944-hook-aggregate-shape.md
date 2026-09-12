---
id: T944
title: hook 计时进程级聚合与健康读面的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

spec 646 的计时是每 HookChain（每会话）私有的——「全进程哪个 hook 最慢」仍要逐会话拼。聚合与读面怎么做？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 48 轮 = effort #600 / spec 647 / impl 500）：`HookTimingAggregator`（core.hook，Holder 模式——RetryBudgetHolder/ToolTimeoutOverrides.Holder 同款）——HookChain.record() 在 aggregator 开启时镜像累计到进程级共享 map（私有链内 stats() 口径不变）。Spring 装配（BuzhouCoreAutoConfiguration）默认开启聚合 + `HookTimingHealth`（BuzhouHealth：UP + details 每 hook {count, totalMicros, maxMicros, avgMicros}——ErrorSignaturesHealth 同构范式）。编程式未装配零变化（aggregator 关=纯私有）。
