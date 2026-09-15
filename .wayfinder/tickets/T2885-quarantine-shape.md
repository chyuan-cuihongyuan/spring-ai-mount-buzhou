---
id: T2885
title: 隔离区普查的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

「放/杀」两极之间的暂存待审怎么读积压？（spec 1842 / effort #1842 / R43）

## Resolution

**邮件隔离区/恶意样本沙箱思想纯读面 `QuarantineCensus`（buzhou-guard）**：
Quarantined(id, reason, age, reviewed) 契约构造 + census（pendingReview/
reviewedCount/oldestPendingAge 无待审 -1 哨兵 + pendingRatio 空区 -1）。
最老旧只看待审侧（已审不积压）；误报有申诉出口、真阳有龄期上限。

