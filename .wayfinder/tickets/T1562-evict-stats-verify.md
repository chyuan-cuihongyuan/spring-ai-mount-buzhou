---
id: T1562
title: evict_handle 逐出判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1561
created: 2026-09-15
---

## Question

J 会话第 53 轮：EvictStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EvictStatsTest，HandleLifecycleRegistry 内存骨架）：合法 spill:// 路径 → evictions=1；http:// 路径 → badPathRejects=1；坏 JSON → parseRejects=1；混合守恒 attempts = evictions + 两拒绝桶；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='EvictStatsTest'` 绿 + 既有 EvictHandleTool 回归绿。
