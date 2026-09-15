---
id: T1847
title: R20 选题——注入快照预算分解断言 + thinkingExtraKeys 经 advisor 贯通 + 文本 null 防御
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 20 轮：注入快照的 budgetBreakdown（tokens.system/user/assistant/total + messages.count）从未被断言；ThinkingChainExtractor extraKeys 厂商扩展经 advisor 的贯通无直接用例；AssistantMessage 文本 null 的防御如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 20 轮 = effort #1219 / spec 1219 / impl 922）：

1. **快照预算分解断言**：Prompt 注入 SYSTEM+USER+ASSISTANT 三角色消息 → adviseCall → PendingSnapshot 的 InjectionSnapshot budgetBreakdown 四键（tokens.system/user/assistant/total，tokenEstimator 长度口径 → 确定性）+ messages.count + SnapshotMessage role 字段（SYSTEM:0/USER:1/ASSISTANT:2 消息 ID 形状）首次直接断言。
2. **thinkingExtraKeys 贯通**：advisor 构造注入 ThinkingChainExtractor(List.of("custom_thinking"), …) → 助手元数据 custom_thinking → THINKING 事件 provider.key=custom_thinking + thinking.available=YES（厂商适配表扩展经 advisor 的端到端贯通）。
3. **文本 null 防御**：AssistantMessage content=null → getText()=null → 无 FINAL_REPLY、无 NPE（recordModelCallOutcome 的 getText null 分支）。
4. **边界**：recordTpotIfNeeded 的 perTokenNs≤0 计时分支不硬凑；不改主代码。
