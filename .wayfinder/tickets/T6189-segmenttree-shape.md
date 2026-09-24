---
id: T6189
title: S 会话 S45 Segment Tree 线段树的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

序列区间和与点更新怎么同时 O(log n)？（spec 5044 /
effort #5044 / S45）

## Resolution

**SegmentTree（core/metrics）**：ZKW 2n 迭代式——叶存原值
父存子区间和；rangeSum/update 双 O(log n)；越界/倒置
fail-fast。
