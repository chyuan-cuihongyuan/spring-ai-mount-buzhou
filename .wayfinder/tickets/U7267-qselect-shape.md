---
id: U7267
title: U 会话 U34 QuickSelect 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

第 k 小怎么最坏 O(n)？（spec 7033 / effort #7033 / U34）

## Resolution

**QuickSelect（core/concurrent）**：五数分组中位数的中位数枢轴+三路分区；副本语义；无随机确定。
