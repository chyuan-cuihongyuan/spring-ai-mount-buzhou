---
id: T2608
title: 裁判位置偏差读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2607
created: 2026-09-15
---

## Question

JudgePositionBias 怎么验证？（spec 1703 验收）

## Resolution

`JudgePositionBiasTest`（core，纯函数直测）：空表哨兵 −1；三类镜像一致各一例
全入 consistent 且 biasRatio=0；首位/次位双赢与混合平分桶 + biasRatio=0.5；
null 按空表。
