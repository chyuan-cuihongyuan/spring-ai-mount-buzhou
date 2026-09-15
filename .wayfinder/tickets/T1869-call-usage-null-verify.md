---
id: T1869
title: R28 adviseCall usage null 保留补测验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1868
created: 2026-09-15
---

## Question

R28 补测后：2 用例是否全绿？observability 模块是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observability 全量）：

1. **定向全绿**：2 新用例（completion-only prompt 跳过/双 null 两属性跳过）。
2. **模块全绿**：observability 155 用例 0 失败 0 错误（新增 2 用例）。
3. 主代码零变化。
