---
id: T1170
title: 上下文截断统计验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1169]
created: 2026-09-13
---

## Question

降序/双累计/溢出净计如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 35 轮 = effort #834）：ContextTruncationStatsTest 3 例——4300 双累计+降序/溢出桶净计 5000+封顶 8+1/脏入参+空真。
