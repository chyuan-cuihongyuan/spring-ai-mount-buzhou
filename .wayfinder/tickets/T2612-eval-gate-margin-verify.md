---
id: T2612
title: 门限边际直方的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2611
created: 2026-09-15
---

## Question

EvalGateMargin 怎么验证？（spec 1705 验收）

## Resolution

`EvalGateMarginTest`（core，纯函数直测）：边际逐项/min/max 正确；带内计数
三档（0/部分/全部）；空表哨兵 −1；threshold 两侧同权；null 按空表。
