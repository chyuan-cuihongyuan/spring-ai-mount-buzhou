# Wayfinder Map — Buzhou 角色工具权限（effort #99，B 会话第 11 轮）

> B 会话第 11 轮。主题池「角色工具权限」：危险工具有 HITL 逐次确认，但「谁能用
> 哪些工具」的<b>面</b>级管控缺失——viewer 只读、editor 可写、admin 全量。
> 借鉴 K8s RBAC（role → 规则集）/ Casbin ACL 面向角色的最小面。

## Destination

ToolPermissions（角色→工具名通配集）+ ToolRoleGuardHook（beforeTool 读会话态
buzhou.tool-role，未授权 block）——未设角色走 default 角色；未定义角色 fail-closed
全拒。挂 hook 即启用。

## Notes

- 号段：B=奇数 spec（本轮 141）。
- order 260（熔断 240 之后、HITL 300 之前——先摘牌再问权再问人）。
- 与 DangerousToolGuardHook 正交：角色管面、HITL 管次。

## Decisions so far

- 通配三形：精确名 / 前缀* / *（全放）。

## Not yet specified

- GuardModule/yml 装配面；角色继承。

## Out of scope

- 沿用 #7–#98；per-arg 细粒度策略（归 PolicyEngine 域）。

## Tickets

- [x] [T491 ToolPermissions 角色通配集 + ToolRoleGuardHook](tickets/T491-tool-roles.md)（impl-283）
- [x] [T492 角色权限回归（通配/态读取/默认/fail-closed/正交）](tickets/T492-tool-roles-tests.md)（impl-283）
