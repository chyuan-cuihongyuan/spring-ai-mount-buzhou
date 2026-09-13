---
id: T1156
title: 定价表覆盖审计验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1155]
created: 2026-09-13
---

## Question

三层匹配/封顶/空真如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 28 轮 = effort #827）：PricingCoverageAuditTest 5 例——三层匹配覆盖率 0.75/典序+封顶+汇总行/全覆盖 1.0/空调用+脏名/空表全未知比率 0。
