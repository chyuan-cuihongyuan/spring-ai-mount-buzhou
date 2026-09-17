---
id: T5007
title: Q 会话 R4 EDF 队列的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

多源时限的「谁最先到期」怎么统一确定性排口？（spec 3003 / effort #3003 / R4）

## Resolution

**EdfScheduler（core/concurrent，单消费者）**：截止期升序 + 同刻
入队序 FIFO tie-break + 空态 +∞（NO_DEADLINE 比较恒安全）+
headLaxity 余量负即超期可判 + Pending(deadline,sequence,id) 全息
记录。EDF 实时调度思想的排队原语化。
