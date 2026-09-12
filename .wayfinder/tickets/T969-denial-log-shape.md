---
id: T969
title: 角色权限拒绝有界日志的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

ToolRoleGuardHook 已有 `buzhou.tool-role.denied` 计数（tag 仅 role——tool tag 违基数守卫）：「谁在反复试哪些无权工具」明细不可见。Redis ACL log（有界最近拒绝 + 每项 reason）怎么映射？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 10 轮 = effort #709 / spec 709 / impl 512）：`ToolDenialLog`（guard 包）——① 有界环形明细（默认 128 条，常量）：{timestampMillis, role, toolName, reason}，reason ∈ unauthorized（有角色无权限）/ undefined-role（fail-closed 未定义）；`entries()` 最新在前不可变快照。② 有界聚合：ConcurrentHashMap<(role,tool), LongAdder> 封顶 64 键（超限 _truncated 布尔），`topDenials()` 不可变快照。ToolRoleGuardHook 增可选构造（log 非空时 beforeTool 拒绝路径双记 reason——既有两参构造行为逐字节不变；计数事件不动）。借鉴 Redis ACL LOG（有界 + 每项 reason + 读面清零语义不做——探针只读）。
