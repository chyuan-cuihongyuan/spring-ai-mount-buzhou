---
id: T2905
title: 截尾均值的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

离群值不污染中枢的稳健均值怎么基建？（spec 1852 / effort #1852 / R53）

## Resolution`

**统计学 trimmed mean/体育评审惯例（去最高最低再平均）纯函数
`TrimmedMean`（core/eval）**：mean(samples, f) 排序后双侧各截 ⌊n×f⌋ 再均
（f∈[0,0.5) 过半无中心语义 fail-fast）；零截退化算术均；f<0.5 数学上
截不空（防御分支不可达入档），空表/null → -1 哨兵；样本 null/NaN
fail-fast。评分清洗与延迟汇报同形状共用。

