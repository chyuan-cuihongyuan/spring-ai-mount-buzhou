---
id: T3213
title: 五数概括与 IQR 围栏的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

分布描述与离群判定怎么稳健口径化？（spec 2056 / effort #2056 / R57）

## Resolution

**Tukey 箱线图纯函数 `FiveNumberSummary`（core/metrics）**：五点
（min/Q1/中位/Q3/max，R-7 线性插值）+IQR+1.5×IQR 围栏（系数可调）
+isOutlier 离群判定（四分位稳健——极端值不拉动围栏，σ 口径自我
掩蔽病的根治）+单点退化 IQR=0 围栏=箱体。
