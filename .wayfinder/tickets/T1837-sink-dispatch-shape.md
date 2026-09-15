---
id: T1837
title: R15 选题——BaseSpanRecorder sink 分发路径（dispatchToSinks 8 missed）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 15 轮：BaseSpanRecorder 的 dispatchToSinks（8 missed：sinks 空/两类 switch 臂/逐 sink 异常隔离/default 跳过）如何补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 15 轮 = effort #1214 / spec 1214 / impl 917）：

1. **补测面**：RecordingBase 带 sinks 构造（3 参 super）——span 分发到达 sink.onSpan（同 SpanRecord）、event 分发到达 sink.onEvent、 throwing sink 异常隔离（落库仍入队 + 第二个 sink 仍收到）、PendingSnapshot 不经旁路 sink（default 臂）、sinks 空时零分发直接落库。
2. **形态**：BaseSpanRecorderSinkDispatchTest 单文件（PipelineSink 录制 stub × 2 + doEnqueue 录制哨兵）。
3. **边界**：不改主代码；OTel 桥（OtelBridgeSink）作为真实 sink 的集成面已由 OtelBridgeMappingTest 覆盖，本轮只测分发骨架。
