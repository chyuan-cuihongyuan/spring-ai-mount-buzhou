# 1612 · guard 孤类装配面（spec 141/167 两 hook 救活）

> 来源：N 会话 R13（effort #1612 / T2375–T2376 / impl 1165）。spec 1611 普查的
> guard hook 家族（Builder 缺开关字段导致零装配路径）修复。

## Problem Statement

ToolRoleGuardHook（spec 141 角色工具权限，K8s RBAC 借鉴——安全边界机制）与
InputFloodGuardHook（spec 167 同输入泛洪防护）建成即孤：不在 GuardModule 注册
列表、Builder 无对应开关字段——宿主无任何装配路径，安全机制等于不存在。

## Solution

GuardModule.Builder 增加：
- `toolRoleGuard(ToolPermissions[, defaultRole])`——声明 permissions 即注册
  ToolRoleGuardHook（fail-closed 语义在 ToolPermissions：未定义角色全拒）；
- `inputFloodGuard([Config])`——声明即注册 InputFloodGuardHook（缺省
  Config.defaults() = 60s 窗 5 次重复）。
未声明双双零注册（默认零行为）。yml 声明面后续轮（编程面先行——安全机制的
最小可用装配路径）。

## Testing Decisions

- `GuardOrphanAssemblyTest` 三断言：permissions 声明 → ToolRoleGuardHook 注册；
  泛洪配置声明 → InputFloodGuardHook 注册；默认构建 → 两 hook 均不注册。
- 回归：guard 全量 334 用例。

## Out of Scope

- yml 声明面（role-guard.permissions map 绑定 / input-flood.* 键）。
- GuardExemptionRegistry（820）的 hook 征询接线——豁免语义面大，独立轮。
