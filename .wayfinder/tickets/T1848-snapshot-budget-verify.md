---
id: T1848
title: R20 补测验证
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1847
created: 2026-09-15
---

## Question

R20 补测后：定向用例是否全绿？observability 模块全量是否绿？

## Resolution

**用户常设授权 AFK（可推翻）**

验证结论（2026-09-15，observability 全量）：

1. **定向全绿**：快照预算四键角色分桶（SYSTEM 4/USER 4/ASSISTANT 4/total 12，长度口径确定性）+ messages.count + role 序列；custom_thinking 经 advisor 贯通 THINKING 事件；content=null 无 FINAL_REPLY 无 NPE。
2. **模块全绿**：observability 全量 0 失败 0 错误（新增 3 用例）。
3. 主代码零变化。
