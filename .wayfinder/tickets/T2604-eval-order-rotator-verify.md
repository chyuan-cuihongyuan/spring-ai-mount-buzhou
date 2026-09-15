---
id: T2604
title: 评测项轮换消序的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2603
created: 2026-09-15
---

## Question

EvalOrderRotator 怎么验证？（spec 1701 验收）

## Resolution

`EvalOrderRotatorTest`（core，纯函数直测）：同 runIndex 确定性；置换多重集
守恒；runIndex 0..7（n=8）两两互异（消序性强断言）；shuffled 内容守恒且
不改入参；n=0/n=1/null 安全。
