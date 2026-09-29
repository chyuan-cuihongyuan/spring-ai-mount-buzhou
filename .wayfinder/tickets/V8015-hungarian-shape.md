---
id: V8015
title: V 会话 V8 HungarianMatcher 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

指派问题怎么摆脱排列爆炸与贪心局部锁死？（spec 8007 / effort #8007 / V8）

## Resolution

**HungarianMatcher（core/policy）**：O(n³) 位势法对偶调整
+增广路交替树；minCost/assignment 双面；方阵语义+并列
取最小列标 canonical。
