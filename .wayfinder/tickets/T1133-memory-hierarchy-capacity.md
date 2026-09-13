---
id: T1133
title: 记忆分层容量读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

三层容量面怎么统一？水位分级与不设限语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 17 轮 = effort #816 / spec 816 / impl 569）：`MemoryHierarchyCapacity` 纯函数——Snapshot 解耦采集；三层固定序+可选 cap（≤0 不设限）；OK/WARN/FULL 三级 80%/100% 边界；脏快照负值归 0；core 恒 1 条目。
