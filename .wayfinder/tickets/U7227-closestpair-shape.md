---
id: U7227
title: U 会话 U14 ClosestPair 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

最近对怎么免全对暴力？（spec 7013 / effort #7013 / U14）

## Resolution

**ClosestPair（core/policy）**：x 排序分治+中带 y 窗口
比较 O(n log²n)；距离平方 long 域；重合 0；±10^9 fail-fast。
