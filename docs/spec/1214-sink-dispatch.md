# 1214 — R15：BaseSpanRecorder sink 分发路径补测

> 来源：K 会话第 15 轮 = effort #1214（[T1837](../../.wayfinder/tickets/T1837-sink-dispatch-shape.md) / [T1838](../../.wayfinder/tickets/T1838-sink-dispatch-verify.md) / impl 917）。方法论：批次化延续——分发骨架（旁路 sink 的 enqueue 时刻回调合同）。

## Problem Statement

BaseSpanRecorder.dispatchToSinks（8 missed）是旁路可观测（OTel 导出桥等 PipelineSink）的分发骨架：enqueue 时刻同步回调、逐 sink 异常隔离、快照/flush-token 跳过。分发回归 = 旁路消费者（OTel addEvent 时序依赖）静默失效。

## 目标

- BaseSpanRecorderSinkDispatchTest（5 用例）：span/event 在 enqueue 时刻到达 sink（同 SpanRecord/EventRecord）；throwing sink 异常隔离（落库不受污染 + 后续 sink 照常收到）；PendingSnapshot 不经旁路（default 臂）；sinks 空时零分发直接落库；open→event→close 入队顺序在旁路分发中保持。

## 实现决策

- RecordingBase 用 3 参 super（meters, includeStacktrace, sinks 列表）注册录制型 PipelineSink × 2（一个可注入抛错）；OTel 真实 sink 集成面由 OtelBridgeMappingTest 覆盖，本轮只测分发骨架。

## 测试决策

- 断言只对可观察行为：sink 收到的调用序列字符串、落库条数；逐 sink 异常隔离的「后续 sink 仍达」为关键不变量。
- 验收门：定向绿 + BaseSpanRecorder 分支 85% 入账 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- ObservabilityAdvisor 细粒度残余（96 missed 中的 captureInjectionSnapshot 细节等，R16 复扫后定）。

## Further Notes

- observability 123 用例全绿。
