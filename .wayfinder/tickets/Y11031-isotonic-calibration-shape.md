---
id: Y11031
title: Y 会话 16 IsotonicCalibration 保序回归 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

IsotonicCalibration（core/metrics，静态纯函数面）：fit(values)——Pool Adjacent Violators 栈式合并非降平方误差保序回归（均值块合并）；总和守恒+null/空/非有限 fail-fast。
