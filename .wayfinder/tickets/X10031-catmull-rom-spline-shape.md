---
id: X10031
title: X 会话 X16 Catmull Rom Spline 样条 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

CatmullRomSpline（core/policy）：均匀 0.5 张力四点三次过点插值+端点重影（Catmull–Rom 思想）。
