---
id: T2915
title: 保工作性审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

多队列调度的「容量白闲」怎么显形？（spec 1857 / effort #1857 / R58）

## Resolution`

**调度理论 work conservation（WFQ/DRR 核心性质）思想纯审计
`WorkConservationAudit`（core/exec）**：Slot 快照契约 + audit 逐时隙判
违例（存在积压队列且存在有配额无积压的闲置队列）→ Report（violations/
totalIdleWithBacklog/totalBacklogDuringViolation + violationRatio/
wasteCoverageRatio——覆盖比 ≥1 即闲置足以清积压纯浪费，-1 哨兵）。
不公平可谈（权重策略），不保工作不可恕。

