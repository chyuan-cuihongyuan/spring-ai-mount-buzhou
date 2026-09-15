---
id: T1865
title: R26 选题——Advisor 细粒度残余（文本 null 防御 + TRM 占位符提取正反例）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 26 轮：ObservabilityAdvisor 细粒度残余（累计 ~70 missed，多数为防御/计时分支）中可达面如何清扫？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 26 轮 = effort #1225 / spec 1225 / impl 928）：

1. **可达面（既有 harness 追加）**：流式侧 AssistantMessage content=null（getText null → reply 跳过不炸）；call 侧 TRM 占位符提取正反例（含 [evidence:]/[spill:] 模式 → SnapshotMessage 字段填充；无模式 → evidenceId/spillUri null——firstMatch null 分支）。
2. **不可达/不硬凑入档**：recordTpotIfNeeded perTokenNs≤0（计时依赖）；markFirstToken subscribedAt==0（doOnSubscribe 前置不可达）；metadata-null 分支（ChatResponse 归一）。
3. **边界**：不改主代码。
