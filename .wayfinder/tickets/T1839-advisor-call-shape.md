---
id: T1839
title: R16 选题——ObservabilityAdvisor 非流式路径（adviseCall/recordModelCallOutcome 22 missed）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 16 轮：R13 流式批次后，ObservabilityAdvisor 剩余缺口大头 recordModelCallOutcome（22 missed，adviseCall 非流式路径）如何补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 16 轮 = effort #1215 / spec 1215 / impl 918）：

1. **补测面（13 用例）**：adviseCall 全链 happy path（THINKING 含 signature/FINAL_REPLY/usage/finish_reason/thinking.available=YES/会话状态聚合经 onTurnEnd 落 TURN 终态）、null response 与 null chatResponse 双层防御、usage 无 completion 归一 0、gpt 启发式/非 gpt/显式 provider 三态、hasToolCalls 与 blank 文本抑制、chain 异常 ERROR+rethrow、截断 payload、snapshotCapture=false 与 sessionSpan=null 两快照跳过分支。
2. **断言基建**：eventTypes（类型序列）+ eventPayloads（record toString 含 payload 布尔标记）双 helper；PendingSnapshot 与 PendingEvent 精确 instanceof 分流（快照捕获开启时同场入队）。
3. **边界**：ToolResponseMessage evidence/spill 分支（构造受限）留批次 5；不改主代码。
