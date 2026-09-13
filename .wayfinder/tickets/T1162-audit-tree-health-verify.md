---
id: T1162
title: 审计树形健康读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1161]
created: 2026-09-13
---

## Question

深度/补位/满树七点边界如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 31 轮 = effort #830）：AuditTreeHealthReadoutTest 3 例——0/1/2/3/4/5/8/9 七点账+1000 大树（深度 10 补位 24）/负数归零。
