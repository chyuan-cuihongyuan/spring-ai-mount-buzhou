---
id: T1841
title: R17 选题——Advisor 流式/非流式残余分支清扫（accumulateStreamChunk 8 + recordModelCallOutcome 14 等）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 17 轮：R16 后 ObservabilityAdvisor 残余 39 missed（accumulateStreamChunk 8/recordModelCallOutcome 14/recordTpotIfNeeded 4/recordStreamOutcome 4/captureInjectionSnapshot 2 等）如何在既有两个 harness 上清扫？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 17 轮 = effort #1216 / spec 1216 / impl 919）：

1. **形态 = 扩展既有两个测试类**（ObservabilityAdvisorStreamTest + ObservabilityAdvisorCallTest 各追加用例），不新建文件——harness 已就绪，边际成本最低。
2. **流式残余**：metadata=null chunk（usage 不捕获不炸）、result=null chunk（空 generations 早退）、finishReason=blank（"  " 不记 finish_reason）、thinking 空串 chunk（stringOf null 跳全表 → 无 THINKING 事件）。
3. **非流式残余**：metadata=null 的 ChatResponse（usage null 分支）、assistant=null（空 generations → 无 thinking/reply、仅 close）。
4. **边界**：firstMatch 与 captureInjectionSnapshot 的 ToolResponseMessage 分支（构造受限）记录豁免；不改主代码。
