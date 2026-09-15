---
id: T2613
title: fork 树形态普查的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ForkShapeStats 的形状怎么裁决？（spec 1706 / effort #1706 / R7）（spec 1706 验收/裁决）

## Resolution

静态纯函数 analyze(childToParent 全集)→ShapeReport(total/roots/forks/maxDepth/maxOutdegree/leafCount)；父 null=根；环路 IllegalArgumentException 诚实拒绝——git DAG 形态普查思想，与 ForkLineageWalker（行走）互补。
