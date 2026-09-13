# 1037 — 工具权限判定分布读面

> 来源：J 会话第 37 轮 = effort #1037（[T1527](../../.wayfinder/tickets/T1527-permission-stats-shape.md) / [T1528](../../.wayfinder/tickets/T1528-permission-stats-verify.md) / impl 789）。与 R19/R26/R30 同族：闸判定分布显形（K8s RBAC audit 思想）。

## Problem Statement

ToolPermissions（spec 141 RBAC fail-closed）allows 判定零计数：未定义角色拒（fail-closed 兜住拼错角色名）与规则不命中拒混在一起——「拒绝率高是角色配置错还是权限真的该拒」不可分；RBAC 规则覆盖（哪些角色从未通过判定）无水位。

## 目标

- `ToolPermissions` 增量（buzhou-guard policy 包，实例级）：`checks` / `allowed` / `deniedUndefinedRole` / `deniedByRules` 四 AtomicLong——守恒不变量 **checks == allowed + deniedUndefinedRole + deniedByRules**。
- 嵌套 record `PermissionStats(long checks, long allowed, long deniedUndefinedRole, long deniedByRules)` + `stats()` 快照。
- allows 返回值逐位不变（null role/tool 仍计 deniedByRules——fail-closed 口径）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按角色分桶命中分布（角色数可控但先验总体分布已足）。
- 判定耗时（hook timing 域）。
