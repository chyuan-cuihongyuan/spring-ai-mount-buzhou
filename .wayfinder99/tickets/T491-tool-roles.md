---
Type: task
Status: closed
---
## Question

角色→工具面：通配集 + 会话态角色 + fail-closed。

## Resolution

done（2026-08-30）：impl-283；ToolPermissions（精确/前缀*/全放三形，未定义角色
全拒）+ ToolRoleGuardHook（order 260，读 buzhou.tool-role，未设走 default）。
