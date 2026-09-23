---
id: T6153
title: S 会话 S27 平滑加权轮询的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

加权分发怎么精确权重比且不扎堆？（spec 5026 / effort #5026 /
S27）

## Resolution

**WeightedRoundRobin（core/policy）**：nginx smooth WRR——
每轮 current += weight，选最大发出并扣 totalWeight；
{5,1,1} 七轮 = aabacaa 不扎堆；currents 读数；确定性。
