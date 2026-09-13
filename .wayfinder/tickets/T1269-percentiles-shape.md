---
id: T1269
title: 评估分数分位数读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 10 轮：EvalScoreAnalytics 只有 min/max/mean（Report）与 bootstrap 区间（spec 903）——排序分位数（P50/P95）读面是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 10 轮 = effort #909 / spec 909 / impl 662）：缺口成立——mean 对长尾不敏感的真相在分位（「P95 多少」是延迟/分数长尾的标准问法）。落点 `EvalScoreAnalytics.percentiles(double[] samples, double... quantiles)`：R-7 线性插值口径（h=(n−1)·q，numpy/Excel 默认——口径显式入档可复现），返回 `LinkedHashMap<Double,Double>` 按入参 q 序（调用方可传 0.5/0.95）；校验 samples 非空 / q ∈ (0,1)。纯函数不触 store；不动 Report record（零破坏）。与 bootstrap（903 区间估计）、passesAtThresholds（747 反事实阈值）三面互补。
