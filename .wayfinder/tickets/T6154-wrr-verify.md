---
id: T6154
title: S 会话 S27 平滑加权轮询的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6153]
created: 2026-09-24
---

## Question

S27 合同怎么逐一验绿？（spec 5026 / effort #5026 / S27）

## Resolution

**验证通过**：WeightedRoundRobinTest 四测全绿——经典 {5,1,1}
七轮序列逐位断言；权重比精确（35 轮频次=权重比）；单节点
退化；空表/weight≤0 fail-fast。
