---
id: T6141
title: S 会话 S21 Bounded Mailbox 有界信箱的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

消息接纳怎么容量显式且溢出代价可观测？（spec 5020 /
effort #5020 / S21）

## Resolution

**BoundedMailbox（core/backpressure）**：Akka bounded mailbox
思想——offer 未满入队、满按策略 DROP_NEWEST 拒新/DROP_OLDEST
逐最旧纳新（droppedCount++）；poll FIFO；size/droppedCount
读数；畸形 fail-fast。
