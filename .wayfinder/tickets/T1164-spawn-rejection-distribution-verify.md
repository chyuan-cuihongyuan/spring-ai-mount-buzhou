---
id: T1164
title: 会话准入拒绝分布验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1163]
created: 2026-09-13
---

## Question

降序/dominant/lastSeen/封顶如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 32 轮 = effort #831）：SpawnRejectionDistributionTest 3 例——5 记录降序+lastSeen max/封顶 16+超封顶不计数/空报告三零。
