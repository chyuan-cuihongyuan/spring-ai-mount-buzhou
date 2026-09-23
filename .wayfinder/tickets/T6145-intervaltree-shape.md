---
id: T6145
title: S 会话 S23 区间树 stabbing 查询的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

区间重叠点查怎么 O(log n+k) 不全量扫？（spec 5022 /
effort #5022 / S23）

## Resolution

**IntervalTree（core/metrics）**：CLRS 居中区间树思想——区间
按中点分桶递归建树，stabbing 点查按点与中点关系走子树 +
节点列表过滤，结果字典序确定性；倒置区间 fail-fast。
