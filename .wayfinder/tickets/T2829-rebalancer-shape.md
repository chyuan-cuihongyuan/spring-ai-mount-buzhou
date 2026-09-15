---
id: T2829
title: 热点重平衡建议器的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

分片/卷间负载倾斜的处置语义怎么安放？（spec 1814 / effort #1814 / R15）

## Resolution

**K8s descheduler 思想纯建议 `HotspotRebalancer`（buzhou-spill）**：
NodeLoad 契约构造 + suggest(quantum, tolerance, loads) 贪心（负载降序并列
id 字典序，极差≤容差停手；单步量 min(quantum, 极差−容差, 极差/2) 不越衡
反转；MAX_MOVES 保险丝）→ RebalancePlan（moves + spreadBefore/After +
improvementRatio -1 哨兵）。确定性可回放；纯建议零执行。

