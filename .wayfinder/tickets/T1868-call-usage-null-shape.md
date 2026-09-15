---
id: T1868
title: R28 选题——adviseCall 路径 usage null 保留两侧补测（NullableUsage）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 28 轮：R16 的 call 路径 usage 用例经 DefaultUsage（null 归一 0）驱动——recordModelCallOutcome 的「attr 跳过」分支（getPromptTokens()==null / getCompletionTokens()==null）在 call 路径仍未覆盖。如何补？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 28 轮 = effort #1225+1 = #1226... 编号按轮次：spec 1227 / impl 930）：

1. **补测面（2 用例）**：completion-only（NullableUsage(null, 3) → prompt 属性跳过、completion 记 3）；双 null（NullableUsage(null, null) → 两属性均跳过）。NullableUsage record（getNativeUsage 桥）沿 R24 流式先例。
2. **边界**：不改主代码；perTokenNs≤0 计时分支豁免不变。
