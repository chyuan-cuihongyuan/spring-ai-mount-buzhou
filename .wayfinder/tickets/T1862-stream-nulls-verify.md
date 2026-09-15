---
id: T1862
title: R24 流式 null 组合补测验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1861
created: 2026-09-15
---

## Question

R24 补测后：3 用例是否全绿？observability 模块全量是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observability 全量）：

1. **定向全绿**：3 新用例（completion-only/全 null 归一 0/0 捕获/null 元素防御跳过）。
2. **模块全绿**：observability 151 用例 0 失败 0 错误（新增 3 用例）。
3. 主代码零变化。诊断插曲入档：DefaultUsage null→0 归一（假红根因）、TotalTokens 返回 Integer（javap 实证）。
