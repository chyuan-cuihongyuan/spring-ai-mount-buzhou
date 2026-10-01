---
id: Y11029
title: Y 会话 15 RansacSampler 随机抽样一致 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

RansacSampler（core/metrics，静态纯函数面）：fitLine(points,iterations,threshold,seed)——二点随机采样+共识内点计数最大胜出（退化样本跳过）；Line 嵌套 record(slope,intercept)+inliers()；迭代/阈值 fail-fast。
