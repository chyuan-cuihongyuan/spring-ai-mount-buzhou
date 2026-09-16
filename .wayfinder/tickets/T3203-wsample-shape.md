---
id: T3203
title: 加权无放回抽样的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

按权重无放回抽 k 个怎么单遍化？（spec 2051 / effort #2051 / R52）

## Resolution

**Efraimidis-Spirakis A-Res 纯函数 `WeightedSample`（core/eval）**：
每元素 key=u^(1/w)（拒绝边界），key 前 k 大即样本——单遍 O(n log k)
数学等价逐次无放回轮盘+权重 0 永不中+k 超池全取（零权除外）+
RandomGenerator 注入回放。
