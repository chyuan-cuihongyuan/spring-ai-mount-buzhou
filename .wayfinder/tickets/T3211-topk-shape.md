---
id: T3211
title: 有界 Top-K 收集器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

流式按值榜单怎么 O(K) 内存化？（spec 2055 / effort #2055 / R56）

## Resolution

**小顶堆守门员 `BoundedTopK<T>`（core/metrics）**：offer 与守门员
（堆顶第 K 名）比——严格大于逐守入门（evicted 计数）否则落选
（rejected 计数），同分先入者保位+top() 降序快照+gatekeeperScore
入榜门槛（空榜 −∞）——O(log K) 推入 O(K) 内存，万级流不存全集。
