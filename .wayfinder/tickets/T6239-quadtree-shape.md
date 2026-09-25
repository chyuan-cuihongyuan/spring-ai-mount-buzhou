---
id: T6239
title: T 会话 T20 QuadTree 四叉树的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

平面区域查询怎么象限剪枝？（spec 6019 /
effort #6019 / T20）

## Resolution

**QuadTree（core/policy，源码 T18 预载）**：桶容量 4 超限
四分（子界与象限判定严格一致），区域查询按矩形-节点界相交
剪枝；结果字典序 canonical；退化单格停分；越界/倒置
fail-fast。
