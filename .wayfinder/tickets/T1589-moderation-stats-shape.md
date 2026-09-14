---
id: T1589
title: 内容安全词表双缝判定读面（ModerationStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1587
created: 2026-09-15
---

## Question

J 会话第 67 轮：guard/moderation 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：ContentModerationHook（内容安全词表：beforeTurn 输入缝 + afterTool 工具输出缝，BLOCK/MASK 双动作）双缝全部只有 micrometer hits 计数——**静默跳过（null/空输入、无命中）与动作分布（BLOCK vs MASK）无进程内直读面**。与 R57 skill_search 同裁决：micrometer 后端面与静态直读面互补（OpenAI moderation 双缝覆盖对账思想）。

形状裁决：`ContentModerationHook` 内静态 `AtomicLong` 五计数——invocations（双缝入口合计）/ blocked（BLOCK 拦截）/ masked（MASK 替换）/ cleanSkips（无命中跳过）/ nullSkips（null/空输入与 error/null 结果跳过）；嵌套 `record ModerationStats` + `stats()` + `resetForTest()`。守恒 `invocations = blocked + masked + cleanSkips + nullSkips`（每入口恰落一桶，双缝共用桶集）。hook 返回与改写语义逐位不变；micrometer 遥测原样保留。

Out of scope：按命中词分桶（词表内容敏感面——红线）；按缝分桶细分（双缝共用桶集口径，缝信息已在 micrometer tag）。
