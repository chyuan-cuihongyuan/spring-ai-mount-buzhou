---
id: T5013
title: Q 会话 R7 P² 流式分位数的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

流式分位数怎么 O(1) 空间在线估计？（spec 3006 / effort #3006 / R7）

## Resolution

**PSquareQuantile（core/metrics）**：Jain-Chlamtac 五标记增量——
min/p/2/p/(1+p)/2/max 位置账面+理想位递推+cell 定位（越界改写端
标记）+内部标记抛物线主路径/线性退路每步 ±1 逼近+前 5 样本排序
缓冲诚实口径+p∈(0,1) 开区间校验。
