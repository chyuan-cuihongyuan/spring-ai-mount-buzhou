# Spec 141 — 角色工具权限（effort #99）

> wayfinder map：`.wayfinder/maps/effort-99.md`（T491–T492）。借鉴：K8s RBAC
> （role → 规则集）/ Casbin RBAC 面——「谁能用哪些工具」的面级管控。

## Problem Statement

危险工具（删库/发版）有 HITL 逐次确认，但日常工具面缺角色维度：给运营开的
会话能调写工具、给只读审计开的会话能调命令执行——「面」级权限（viewer 只读 /
editor 可写 / admin 全量）每家都在业务层手搓。

## Solution

`ToolPermissions`（guard/policy）+ `ToolRoleGuardHook`：

- **规则**：角色 → 工具名通配集（三形：精确名 `read_file` / 前缀 `log*` / 全放 `*`）。
- **角色解析**：会话态键 `buzhou.tool-role`（宿主在会话建立/登录态写入）；
  未设 = `default` 角色（构造可配默认角色名）。
- **裁决**：beforeTool（order 260）——角色未定义 <b>fail-closed 全拒</b>（拼错
  角色名不是放行理由）；未授权 → `HookResult.block`（角色名+工具名+修法提示）。
- 计数 `buzhou.tool-role.denied`（role tag 有界——角色集本就有界）。
- 与 DangerousToolGuardHook 正交：角色管「面」，HITL 管「次」——admin 过了角色
  关，危险工具仍要人工确认。

## User Stories

1. 作为宿主，我按角色声明工具面（viewer 只读/log*，editor 加写，admin *），
   会话态写一个角色名即完成绑定。
2. 作为运维，拼错角色名 = 全拒（fail-closed）——权限系统的失败方向必须安全。
3. 作为宿主，不写角色键的存量会话走 default 角色——默认面归宿主定义。

## Implementation Decisions

- 角色集有界（构造期确定）；通配编译为前缀/精确判定，不做正则（防 ReDoS 面）。
- hook 不缓存角色（每 beforeTool 读会话态——角色可在会话中途升/降级即时生效）。

## Testing Decisions

- 通配三形 + 边界（log* 不匹配 login_check 的歧义——前缀按字符）；角色从态读取；
  未设走 default；未定义角色全拒；block 文案含角色/工具；允许路径 CONTINUE。
- 先例：PiiYmlCustomRulesTest（ctx 驱动 + HookEnvironment）。

## Out of Scope

- yml 装配面；角色继承/组合；per-arg 策略（PolicyEngine 域）。

## Further Notes

- 护栏三件套：角色面（本轮）+ HITL 次（spec 07）+ 策略引擎细粒度（既有）。
