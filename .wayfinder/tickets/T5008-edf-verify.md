---
id: T5008
title: Q 会话 R4 EDF 队列的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5007]
created: 2026-09-18
---

## Question

R4 合同怎么逐一验绿？（spec 3003 / effort #3003 / R4）

## Resolution

**验证通过**：EdfSchedulerTest 八测全绿——乱序入队按截止期出、
同刻三任务 FIFO、空态四读一致（poll/peek null + nextDeadline
+∞ + size 0）、peek 非破坏、laxity 60/0/−50 三段（负即超期）、
负截止期过期先出、Pending 全息（deadline/id/sequence 单调）、
size/isEmpty 生命周期守恒。
