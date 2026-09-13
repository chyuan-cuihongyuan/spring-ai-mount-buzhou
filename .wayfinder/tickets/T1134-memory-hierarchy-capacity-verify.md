---
id: T1134
title: 记忆分层容量读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1133]
created: 2026-09-13
---

## Question

层序/水位边界/混排设限如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 17 轮 = effort #816）：MemoryHierarchyCapacityTest 4 例——三层 items/chars/total/不设限 null 三断言/水位四点（0.8 WARN、1.0 FULL、0.799 OK、1.2 FULL）/混排（0.5 OK、0.85 WARN、null cap）/脏快照归零+core 恒 1。
