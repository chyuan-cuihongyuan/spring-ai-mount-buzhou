---
id: T6042
title: R 会话 R21 协作预算的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6041]
created: 2026-09-23
---

## Question

R21 合同怎么逐一验绿？（spec 4020 / effort #4020 / R21）

## Resolution

**验证通过**：CoopBudgetTest 五测全绿——3 点扣至恰尽 hasBudget
翻假；8 点尽后 yield 重置满额；尽后再扣 IllegalStateException +
重置后恢复可扣；双任务账户互不串门；畸形三型 fail-fast（0/负）。
