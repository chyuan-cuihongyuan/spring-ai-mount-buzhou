---
id: T2866
title: 覆写环形缓冲的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2865]
created: 2026-09-16
---

## Question]

环形窗在 FIFO/覆写/退化/拷贝/畸形五面下正确吗？（spec 1832 / effort #1832 / R33）

## Resolution

**OverwritingRingBufferTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=OverwritingRingBufferTest）：容量内 FIFO；5 入 3 容 → [3,4,5] 且
overwrites=2；容量 1 只留最新；items 防御拷贝（快照后追加不串读）；
容量<1/null 元素 fail-fast。

