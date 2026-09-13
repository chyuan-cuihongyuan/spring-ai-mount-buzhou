---
id: T1528
title: 工具权限判定分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1527
created: 2026-09-14
---

## Question

J 会话第 37 轮：权限判定分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（PermissionStatsTest，AssertJ 同仓风格）：精确名/前缀/全放三形 allowed 各计；未定义角色 deniedUndefinedRole=1；已定义角色无匹配模式 deniedByRules=1；null role/tool 计 deniedUndefinedRole（fail-closed 口径）；守恒 checks == allowed + 两拒和；fresh 零值。定向 `mvn -pl buzhou-guard test -Dtest='PermissionStatsTest'` 绿 + 既有 ToolPermissions 回归绿。
