---
id: U7221
title: U 会话 U11 LcaLifting 倍增 LCA 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

树祖先查询怎么 O(log n)？（spec 7010 / effort #7010 / U11）

## Resolution

**LcaLifting（core/concurrent）**：up[k][u] 倍增表
O(n log n) 预处理；深度对齐+同步上跳；构建期树形校验。
