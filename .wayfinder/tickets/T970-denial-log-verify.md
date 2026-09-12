---
id: T970
title: 角色权限拒绝有界日志的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T969
created: 2026-09-13
---

## Question

拒绝真双记（明细 + 聚合）？reason 分流正确？有界不爆？默认构造零回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 10 轮）：① 未授权拒绝 → entries 头部 {role,tool,unauthorized} + topDenials (role,tool) +1；未定义角色 → reason=undefined-role；② 129 条拒绝 → 环形封顶 128（最老被挤）；65 个 (role,tool) 对 → truncated 标记；③ 既有 ToolRoleGuardHook 用例（无 log 构造）零回归；④ entries/topDenials 不可变。`mvn -pl buzhou-guard -am test` 全绿。
