---
id: T2921
title: 对数分桶直方图的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

分位数怎么有界误差一遍出？（spec 1860 / effort #1860 / R61）

## Resolution`

**HdrHistogram/DDSketch 思想纯函数 `LogBucketHistogram`
（core/metrics）**：bucketIndex=floor(ln v/ln γ)（桶内相对差 ≤ γ−1）+
buckets TreeMap 桶序账 + quantile 桶序累计取几何中点 →
ApproxQuantile(estimate, relativeErrorBound=γ−1)——误差界声明在返回值；
正值域契约、DEFAULT_GAMMA=1.25（±12.5%）。O(n) 免全排序、高段不失真
（对照线性桶）。

