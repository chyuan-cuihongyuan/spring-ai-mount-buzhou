---
id: T6237
title: T 会话 T19 KD-Tree 二维最近邻的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

二维最近邻怎么不全点集扫？（spec 6018 /
effort #6018 / T19）

## Resolution

**KdTree（core/policy，源码 T18 预载）**：交替轴中位数分割
静态树+回溯剪枝（轴平面距离平方≤最优才探另一侧）；long
坐标距离平方；平局 canonical（dist→x→y）；null/空/畸形
fail-fast。
