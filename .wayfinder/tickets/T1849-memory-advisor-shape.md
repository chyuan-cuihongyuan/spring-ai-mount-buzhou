---
id: T1849
title: R21 选题——BuzhouMemoryAdvisor 分支直测（写路径 fence/去重/角色过滤）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 21 轮：批次 4 收尾与 advisor 家族直测——BuzhouMemoryAdvisor（10 missed，写路径 fence 双主窗口零写入承诺）与 HookAdvisor（6 missed）如何切批？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 21 轮 = effort #1220 / spec 1220 / impl 923）：

1. **本批 = BuzhouMemoryAdvisor 单类**（写路径 fence 是双主窗口零写入承诺的执行面，行为敏感优先）；HookAdvisor（6 missed，beforeModel/onModelError 切面分发）留 R22 单做（HookResult stub 家族需独立设计）。
2. **补测面（8 用例）**：conversationId 缺失直通；USER+ToolResponseMessage 写入与 prompt 重建；instructions 内 ASSISTANT 过滤；同实例去重不重写（Identity 语义）；fence 先于落库 + 抛错阻断零写入 + 恢复后照常；after 落 assistant（fence 一次）；null chatResponse 与 conversationId 缺失 no-op；无 fence（null = 无租约语义路径）行为不变。
3. **形态**：BuzhouChatMemory/InMemoryMessageStore 真件复用（无 Mockito）；ToolResponseMessage 经 builder（构造 protected）。
4. **边界**：不改主代码；HookAdvisor 留 R22。
