---
id: T6209
title: T 会话 T5 Monotonic Deque 单调队列的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-25
---

## Question

固定窗口最值怎么每元素摊还 O(1)？（spec 6004 /
effort #6004 / T5）

## Resolution

**MonotonicDeque（core/metrics）**：定容窗+队内值严格递减
（队尾 ≤ 新值弹出、队首越窗弹出）——队首恒为窗最大，每元素
至多入队/出队各一次（摊还 O(1) 无墓碑）；max/size 读数；
capacity≤0 fail-fast。
