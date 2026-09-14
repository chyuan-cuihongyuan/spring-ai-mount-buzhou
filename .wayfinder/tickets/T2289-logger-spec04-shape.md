---
id: T2289
title: System.Logger 双门面追认 + spec 04 mcp 属性回写的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 22 轮：五-3（System.Logger vs SLF4J 分叉）与四-7（spec 04 增量未回写）如何裁定？

## Resolution

**用户常设授权 AFK（可推翻）**

① 五-3：**双门面追认**——仓内 69 个 main 文件已用 System.Logger（含历轮全部会话产出，属既成风格而非零星违规）；迁移 SLF4J 零功能收益且有 69 文件扰动风险。CLAUDE.md 规约措辞修正（「统一 SLF4J」→「SLF4J 或 System.Logger 双门面皆可、占位符风格强制、同文件不混用」）+ spec 13 §11 同口径注记。占位符风格（禁止拼接、禁止丢栈）的硬约束不变。
② 四-7：spec 04 模块归属段补 mcp 装配属性增量回写（dangerous-tool-patterns 缺省七动词（spec 1507）/shutdown-budget 35s/per-connection-concurrency-limit（spec 628）+ 指针 docs/config-reference.md）；skills 注释指针维持现状（指向后续档语义正确）。
