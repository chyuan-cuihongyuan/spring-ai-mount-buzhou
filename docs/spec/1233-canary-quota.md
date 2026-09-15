# 1233 — R34：金丝雀候选限流放行与配额窗口语义

> 来源：K 会话第 34 轮 = effort #1233（[T1873](../../.wayfinder/tickets/T1873-canary-quota-shape.md) / [T1874](../../.wayfinder/tickets/T1874-canary-quota-verify.md) / impl 934 续）。方法论：行为语义选题——候选级限流的放行/耗尽两态。

## Problem Statement

金丝雀候选级限流（candidateLimiter acquireOrThrow）的窗口语义从未被直接断言：InMemoryRateLimitBackend 的 RPM 窗口为全局限流（非按模型分键），耗尽后金丝雀与主路同窗受限——「按模型独立配额」直觉与实际语义存在偏差，需用例钉死实际合同。

## 目标

- CanaryQuotaExhaustedTest 两用例：放行态（RPM=10 额度内双轮金丝雀照常直达备模型，主模型零调用）与耗尽态（RPM=1 时次轮 acquireOrThrow 抛 ModelRateLimitExceededException——全局限流窗语义实证）。

## 实现决策

- 纯函数静态方法直测（无状态无 Mockito）；先例：FallbackChainEndToEndTest 多模型 harness。

## 测试决策

- 断言只对外部行为：回复内容、事件类型与 payload、异常类型。
- 验收门：定向绿 + resilience 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- ResilienceAdvisor 深水区（891 行，独立深做轮）。

## Further Notes

- 「签名输入规范化」域与前序轮的共通教训：签名链的输入合同（规范化、序列化）是安全面最高频的静默失效点——测试即合同。
