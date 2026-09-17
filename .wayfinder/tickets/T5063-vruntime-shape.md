---
id: T5063
title: Q 会话 R32 vruntime 公平队列的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

异质实体怎么按权重动态公平分吞吐？（spec 3031 / effort #3031 / R32）

## Resolution

**VirtualRuntimeQueue（core/concurrent）**：CFS vruntime——虚拟运行
时间按权折算（vruntime += work/weight，重权者慢走），恒取最小者
（TreeMap O(log n) 红黑树同构）；等权退化轮转、w 倍权 w 倍配额。
并列先入先选确定性；新注册从 0 起的追赶份额诚实口径声明（CFS
min_vruntime 对齐留白）。
