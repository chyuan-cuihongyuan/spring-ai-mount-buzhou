# 1216 — R17：Advisor 流式/非流式残余分支清扫

> 来源：K 会话第 17 轮 = effort #1216（[T1841](../../.wayfinder/tickets/T1841-residual-sweep-shape.md) / [T1842](../../.wayfinder/tickets/T1842-residual-sweep-verify.md) / impl 919）。方法论：harness 复用的残余清扫——R13/R16 落地的流式与非流式 harness 上追加边缘用例，零新基建。

## Problem Statement

R13/R16 后 ObservabilityAdvisor 残余 39 missed 集中在防御分支：accumulateStreamChunk 的 metadata=null / result=null / finishReason blank / thinking 空串，recordModelCallOutcome 的 metadata=null（usage null）/ assistant=null（空 generations），markFirstTokenIfNeeded 与 recordTpotIfNeeded 的早退分支组合。

## 目标

- 流式（ObservabilityAdvisorStreamTest 追加）：metadata=null chunk 不捕获 usage 不炸；空 generations chunk 早退；blank finishReason 不记 finish_reason；空串 thinking chunk 无 THINKING 事件且 outcome 标 NO。
- 非流式（ObservabilityAdvisorCallTest 追加）：metadata=null ChatResponse → usage 跳过、thinking/reply 照常；空 generations → 仅关闭无事件。

## 实现决策

- 全部用例落入既有两测试类（harness 复用，零新基建）；主代码零变化。

## 测试决策

- 断言只对外部行为：事件类型序列、span 属性、异常不外泄。
- 验收门：定向绿 + ObservabilityAdvisor 残余 missed 收敛入账 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- firstMatch / captureInjectionSnapshot 的 ToolResponseMessage 分支（构造受限，记录豁免）。

## Further Notes

- 防御分支（上游契约保证非空的路径）如实记录不硬凑。
