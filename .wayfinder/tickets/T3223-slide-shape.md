---
id: T3223
title: 单调队列滑窗极值的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

定长滑窗极值怎么 O(1) 摊销？（spec 2061 / effort #2061 / R62）

## Resolution

**单调双端队列 `SlidingExtremum`（core/metrics）**：offer 入队前弹
永无出头之日的队尾（max：≤ 新者——更老且不大，滑出后也轮不到），
队首恒窗极值；序号自滑出免窗口数组；max/min 双口径+空窗 NaN+候选
计数（压制弹出者不在内）——朴素 O(W) 重扫的根治。
