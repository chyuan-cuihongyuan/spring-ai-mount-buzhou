---
id: T6197
title: S 会话 S49 Pairing Heap 配对堆的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

两个优先队列怎么 O(1) 合一且弹出摊还对数？（spec 5048 /
effort #5048 / S49）

## Resolution

**PairingHeap（core/concurrent）**：Fredman-Sedgewick 思想
——多叉最小堆、insert/meld 根比较挂钩 O(1)（所有权清空）、
extractMin 后孩子两趟合并（两两配对+逆序并回）；同操作
序列同出序；空堆/self-meld fail-fast。
