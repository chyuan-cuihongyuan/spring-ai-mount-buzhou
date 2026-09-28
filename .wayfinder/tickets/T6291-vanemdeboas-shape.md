---
id: T6291
title: T 会话 T46 VanEmdeBoas 有界宇宙树的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-28
---

## Question

小整数全域后继怎么 O(log log u)？（spec 6046 / effort #6046 / T46）

## Resolution

**VanEmdeBoas（core/concurrent）**：2^bits 半位分簇
递归+summary 摘要；min 镜像/max 冗余（CLRS）；簇惰性创建；
insert 幂等/delete 缺席 fail-fast/successor 缺席 -1。
