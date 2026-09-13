---
id: T1477
title: Hook Replace 载荷应用/丢弃计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 14 轮：Hook Replace 载荷应用/丢弃计数读面（幽灵载荷显形）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 14 轮 = effort #1013 / spec 1013 / impl 766）：缺口成立——`HookChain.applyReplace` 的类型分支与 `default` 臂存在**静默丢弃**路径（如 beforeTurn 返回 Replace(42) 非 String 载荷、beforeModel 返回非 ChatClientRequest/Response 载荷）：替换意图无声蒸发，钩子作者与运维均无信号（与 R3 幽灵禁用同族的静默蒸发显形）。落点 core.hook：applyReplace 改返回 boolean（applied），HookChain 增实例级 `replaceApplied`/`replaceDropped` 两 AtomicLong + `replaceAppliedCount()`/`replaceDroppedCount()` 读面；分发行为逐位不变（丢弃仍静默不抛——只显形不拦截）。
