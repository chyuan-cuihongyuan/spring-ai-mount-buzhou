---
id: U7205
title: U 会话 U3 SqrtDecomposition 分块的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

区间和怎么点更 O(1)？（spec 7002 / effort #7002 / U3）

## Resolution

**SqrtDecomposition（core/metrics）**：⌈√n⌉ 分块块和
摘要——点更 O(1) 差分回写、区间和 O(√n) 直摘；倒置/
越域/空列 fail-fast。
