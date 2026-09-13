---
id: T1527
title: 工具权限判定分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 37 轮：工具权限判定分布读面（K8s RBAC audit 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 37 轮 = effort #1037 / spec 1037 / impl 789）：缺口成立——ToolPermissions（spec 141 RBAC fail-closed）allows 判定全程零计数：判了多少次、允许多少、**未定义角色拒**（拼错角色名被 fail-closed 兜住）与**规则不命中拒**各多少不可见——「拒绝全是拼错角色」即配置错误信号。落点 buzhou-guard policy 包：实例级 checks/allowed/deniedUndefinedRole/deniedByRules 四 AtomicLong（守恒 checks == allowed + 两类拒）+ 嵌套 record `PermissionStats` + `stats()`。实例级；嵌套类型不动 API 快照；allows 返回值逐位不变。
