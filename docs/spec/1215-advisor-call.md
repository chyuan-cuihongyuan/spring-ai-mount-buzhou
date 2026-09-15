# 1215 — R16：ObservabilityAdvisor 非流式路径分支补测（adviseCall × recordModelCallOutcome）

> 来源：K 会话第 16 轮 = effort #1215（[T1839](../../.wayfinder/tickets/T1839-advisor-call-shape.md) / [T1840](../../.wayfinder/tickets/T1840-advisor-call-verify.md) / impl 918）。方法论：R13 流式批次延续——adviseCall 路径（recordModelCallOutcome 22 missed 集中区）的 adviseCall 全链驱动。

## Problem Statement

recordModelCallOutcome（非流式 MODEL_CALL 收口）22 missed：usage 三态、thinking 三态（PROVIDER_NOT_RETURNED 启发式/显式 provider 覆盖/NO）、toolCalls 与 blank 文本对 FINAL_REPLY 的抑制、null response 双层防御、chain 异常标 ERROR——非流式调用是生产主路径（adviseCall），此前零直接测试。

## 目标

- ObservabilityAdvisorCallTest（13 用例）：happy path（THINKING 含 signature/FINAL_REPLY/usage/finish_reason/thinking.available=YES/会话状态 usage 聚合经 onTurnEnd 落 TURN 终态）；null response 与 null chatResponse 双层防御（仅关闭无事件）；usage 无 completion → DefaultUsage 归一 0；无思维链 gpt 启发式 → PROVIDER_NOT_RETURNED、非 gpt → NO、显式 modelProvider=openai 覆盖启发式；hasToolCalls 与 blank 文本抑制 FINAL_REPLY；chain 异常 → ERROR 终态 + rethrow；snapshotCapture=false 与 sessionSpan=null 两快照跳过分支；截断 payload 带 truncated + original.length。

## 实现决策

- CallAdvisorChain 匿名 stub（nextCall/getCallAdvisors/copy）；ChatClientResponse(ChatResponse, Map) record 直接构造。
- 事件断言双 helper：eventTypes()（类型序列）+ eventPayloads()（record toString 含 payload 细节——truncated 等布尔标记在类型名中不可见，此为一次假红根因）。
- state.onTurnEnd 落 TURN 终态后，聚合 usage 断言取**最后一条** PendingSpan（TURN 终态 upsert，非开态记录）。

## 测试决策

- 断言只对外部行为：span 属性/状态/事件类型与 payload 标记；PendingSnapshot 与 PendingEvent 精确 instanceof 分流（快照同场入队）。
- 验收门：定向绿 + ObservabilityAdvisor 分支 69%→75%（隔离 worktree 累计口径保守值）+ observability 全量绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- captureInjectionSnapshot 的 ToolResponseMessage evidence/spill 提取分支（ToolResponseMessage 构造受限——protected，留批次 5 评估）。

## Further Notes

- observability 136 用例全绿（新增 13）。
