---
id: T2865
title: 覆写环形缓冲的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

最近窗采样的「不挡主路+丢得起有数」基建怎么安放？（spec 1832 / effort #1832 / R33）

## Resolution

**LMAX Disruptor 思想 `OverwritingRingBuffer`（core/concurrent）**：add 满则
覆写最老（overwrites 计数可审计）+ items 最老到最新序防御拷贝快照 +
stats(capacity/size/overwrites/hasOverwritten)；capacity<1 与 null 元素
fail-fast。环形头指针单点推进，synchronized 小临界区原子。

