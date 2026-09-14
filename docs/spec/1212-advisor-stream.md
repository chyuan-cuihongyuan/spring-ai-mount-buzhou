# 1212 — R13：ObservabilityAdvisor 流式路径分支补测

> 来源：K 会话第 13 轮 = effort #1212（[T1833](../../.wayfinder/tickets/T1833-advisor-stream-shape.md) / [T1834](../../.wayfinder/tickets/T1834-advisor-stream-verify.md) / impl 915）。方法论：批次化延续 + harness-first——本仓首个 Spring AI 流式 advisor 测试基建（此前 86 covered 分支全部来自非流式路径）。

## Problem Statement

ObservabilityAdvisor 的流式回调链（adviseStream → accumulateStreamChunk → markFirstTokenIfNeeded → recordTpotIfNeeded → recordStreamOutcome，68 missed 集中区）从未被直接测试：TTFT 首信号幂等、TPOT 三态跳过、sawToolCalls 对 FINAL_REPLY 的抑制、流错/取消终态、resolveTurnParent 三级回退——全部是流式可观测口径的核心分支。

## 目标

- **ObservabilityAdvisorStreamTest**（7 用例，harness 从零搭建）：
  - happy path：THINKING 聚合事件 + FINAL_REPLY + STREAM_FIRST_TOKEN + usage/finish_reason/tpot.ms 属性 + OK 终态（4 span：SESSION/TURN/MODEL_CALL RUNNING/终态）；
  - usage-only 流：无 TTFT 属性（首内容口径）、usage 属性仍记；
  - sawToolCalls：抑制 FINAL_REPLY、finish_reason=tool_calls 记录；
  - completionTokens=null：usage 记 prompt 桶、TPOT 跳过；
  - 流错：span 置 ERROR + 关闭 + 异常透传订阅方；
  - 取消：take(1) 取消 → CANCELLED 终态（杜绝 RUNNING 孤儿）；
  - resolveTurnParent 三级回退：turn → session 派生 → null。

## 实现决策

- harness（javap 实证 Spring AI 2.0.1 构造面）：StreamAdvisorChain 匿名 stub（3 方法）；ChatClientRequest(Prompt, Map) 2 参 record；ChatResponseMetadata.builder().usage(DefaultUsage)；ChatGenerationMetadata.builder().finishReason()；AssistantMessage.builder().properties().toolCalls()。
- recorder = RecordingBase extends BaseSpanRecorder（super 双参，doEnqueue 录制 PendingItem）；事件断言经 PendingEvent instanceof 过滤（PendingSnapshot 因 snapshotCapture=true 同场入队）。
- TokenEstimator 三方法匿名 stub（非函数接口，不能用 lambda）。

## 测试决策

- 断言只对可观察行为：导出的 span 状态/属性/事件类型序列与异常透传；不断言 Flux 内部信号时序。
- 验收门：定向绿 + ObservabilityAdvisor 分支覆盖提升入账 + observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- captureInjectionSnapshot 细节分支（快照捕获已随 happy path 间接执行；细粒度留批次 5）。
- 他线（I/J/M/N）在跑主题。

## Further Notes

- 本轮后 K 线具备 Spring AI 流式 advisor 的测试基建先例——后续 Memory/ToolCalling 等 advisor 流式补测可直接复用 harness 形态。
