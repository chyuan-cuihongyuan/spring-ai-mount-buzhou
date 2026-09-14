---
id: T2295
title: 评估并行路径失败率剪枝做实（spec 901 边界收口）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2261
created: 2026-09-15
---

## Question

M 会话第 25 轮：spec 901 剪枝的「并行路径诚实不生效」（invokeAll 无低成本中途取消——入档边界）如何收口？

## Resolution

**用户常设授权 AFK（可推翻）**

形状：分波提交（wave execution——Rayon/数据流引擎的 cooperative batching 思想）：并行路径把 items 按 workers 分块（波次），每波 invokeAll 后检查失败率——达阈值（同串行观察窗语义：已完成 ≥ minItems 且 fail+error 占比 ≥ threshold）则剩余全部标 pruned 不再起波。opt-in 语义不变（未配 EvalPrunePolicy 零行为——单波跑完等价既有 invokeAll 全量）；串行路径不动；取消（spec 1505）与剪枝正交并存（取消优先检查）。
