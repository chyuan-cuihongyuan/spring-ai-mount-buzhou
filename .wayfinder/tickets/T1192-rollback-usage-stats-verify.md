---
id: T1192
title: Prompt 回滚使用读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1191]
created: 2026-09-13
---

## Question

计数/最近版本对/溢出如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 46 轮 = effort #845）：RollbackUsageStatsTest 3 例——2:1 降序+版本对+lastSeen/溢出桶 64+1+total/脏入参+空真。
