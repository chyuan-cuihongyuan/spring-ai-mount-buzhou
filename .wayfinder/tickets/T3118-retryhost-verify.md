---
id: T3118
title: 重试主机排除的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3117]
created: 2026-09-17
---

## Question

RetryHostExclusion 合同（让位/回归/回退/累积/序/顺延/畸形）怎么钉住？（spec 2008 / effort #2008 / R9）

## Resolution

**七用例全绿**（首跑 2 红根因：单候选用例触发「全排除回退全量」特性
与空期望冲突——回退是设计语义非缺陷，双候选改写后 7/7）：失败让位 /
到期回归（恰 1000ms 边界）/ 全排除回退+计数显形 / 多失败独立回归 /
序保持 / 再失败冷却顺延 / 畸形五型 fail-fast。
