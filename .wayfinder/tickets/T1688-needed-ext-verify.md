---
id: T1688
title: 预算 needed 判定分布并入的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1687
created: 2026-09-15
---

## Question

J 会话第 114 轮：needed 分布如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（BudgetClampStatsTest 增用例）：needed=true 场景（大 history）→ neededTrue=1；小输入 → neededFalse=1。定向 `mvn -pl buzhou-memory -am test -Dtest='BudgetClampStatsTest'` 绿 + 既有预算回归绿。
