---
id: T5005
title: Q 会话 R3 并查集的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

动态等价类传递闭包怎么均摊近常量维护？（spec 3002 / effort #3002 / R3）

## Resolution

**DisjointSet（core/concurrent，定容 int 宇宙）**：find 路径减半
+union 按秩挂接（Tarjan 均摊 α(n)）+冗余合并 false 不动账面+
componentCount 守恒读数+sizeOf 聚合+越界 fail-fast。「A=B、B=C ⇒
A=C」传递闭包免 O(n²) 重扫的根治件。
