---
id: T6043
title: R 会话 R22 工作窃取的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

存量负载失衡怎么搬而非只管新任务去哪？（spec 4021 / effort #4021 / R22）

## Resolution

**WorkStealingSplit（core/concurrent）**：Cilk 双端队列 + steal-half
——owner 热端 LIFO、闲工冷端偷对半（下限 minSteal），一次摊平梯度。
纯裁决件（真并发归运行时）。与 TwoChoiceSelector（到达择短）互补。
