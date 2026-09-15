---
id: T2606
title: 评测集覆盖矩阵的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2605
created: 2026-09-15
---

## Question

EvalCoverageMatrix 怎么验证？（spec 1702 验收）

## Resolution

`EvalCoverageMatrixTest`（core，纯函数直测）：计数与去重；missingFrom 差集
字典序；熵契约（单标签 0/两标签均衡≈1/偏科严格小于中间态/界 0..1）；
null 与无标签项计数口径。
