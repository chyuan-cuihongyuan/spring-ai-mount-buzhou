---
id: T2683
title: 注入分类校准探针的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

InjectionCalibrationProbe 的形状怎么裁决？（spec 1741 / effort #1741 / R42）（spec 1741 验收/裁决）

## Resolution

实例面 record(predicted, actually) 四象限 TP/TN/FP/FN+report(precision/recall/accuracy 分母 0 哨兵 −1)+resetForTest——HF evaluate 混淆矩阵思想，PiiProbeSelfCheck 同族。
