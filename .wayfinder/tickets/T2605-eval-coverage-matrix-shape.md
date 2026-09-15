---
id: T2605
title: 评测集覆盖矩阵的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

评测集标签覆盖的形状怎么裁决？（spec 1702 / effort #1702 / R3）

## Resolution

**静态纯函数 `EvalCoverageMatrix`（core/eval）**：`build(itemLabelSets)` →
`CoverageReport(itemCount/labelCounts 字典序/distinctLabels)`；
`missingFrom(universe)` 漏测清单；`shannonEntropy()` 归一化熵 0..1
（ln k 归一，k≤1 记 0）。借鉴 JaCoCo/Stryker 覆盖矩阵 + scikit-learn 信息熵。
标签宇宙由宿主声明——纯读面零体系强加。
