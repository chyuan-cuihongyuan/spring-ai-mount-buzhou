---
id: T1686
title: 预算钳位读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1685
created: 2026-09-15
---

## Question

J 会话第 113 轮：BudgetClampStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（BudgetClampStatsTest，桩 windowResolver/estimator 骨架——见既有预算测试）：正常输入 → normalBudgets=1；超大 fixedOverhead → negativeClamps=1；守恒 evaluations = 两桶和；resetForTest 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='BudgetClampStatsTest'` 绿 + 既有预算回归绿。
