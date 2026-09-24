---
id: T6169
title: S 会话 S35 Radix Tree 基数树最长前缀路由的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

前缀路由怎么压缩结构且每查只走键深？（spec 5034 /
effort #5034 / S35）

## Resolution

**RadixTree<V>（core/policy）**：go-chi/httprouter 压缩前缀
树思想——单字符边合并字符串边、insert 部分命中分裂中间
节点（终态与插入序无关）、match 最长前缀（尽深最近终节点
胜出）；keyCount/nodeCount 读数；畸形 fail-fast。
