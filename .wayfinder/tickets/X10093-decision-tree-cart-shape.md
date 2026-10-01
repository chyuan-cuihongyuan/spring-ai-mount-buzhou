---
id: X10093
title: X 会话 47 DecisionTreeCart CART 基尼树 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

DecisionTreeCart（core/metrics）：fit(features,labels,classCount,maxDepth,minSamplesLeaf)——逐特征排序中点阈值候选+加权基尼严格下降贪心分裂（平局先到先得确定序）+纯/深限/最小叶三停机；嵌套 Node 树+predict 走树；null/标签越界/超参 fail-fast。
