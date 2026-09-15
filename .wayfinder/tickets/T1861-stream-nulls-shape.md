---
id: T1861
title: R24 选题——Advisor 流式 usage 组合与 null-chatResponse 防御（3 用例）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 24 轮：流式 usage 组合的 null 侧分支（completion-only/全 null/流中 null chatResponse 元素）如何定向补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 24 轮 = effort #1223 / spec 1223 / impl 926）：

1. **补测面（3 用例，流式 harness 追加）**：completion-only usage（prompt 侧不记）；全 null Usage 元数据 → 经 builder 归一 0/0 捕获（实证 ChatResponseMetadata.builder().usage() 对 NullableUsage 做归一）；流中 null chatResponse 元素被 doOnEach 防御跳过不炸、后续 chunk 照常。
2. **诊断插曲入档**：初版经 DefaultUsage(null,…) 驱动 null 分支假红——DefaultUsage 将 null 归一 0（打点实证 attrs 0/0）；改 NullableUsage record（补 getNativeUsage）后行为面真实验证。TotalTokens 返回类型为 Integer（非 Long）——javap 实证。
3. **边界**：不改主代码。
