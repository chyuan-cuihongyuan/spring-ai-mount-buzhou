---
id: T6277
title: T 会话 T39 Weighted Reservoir Sampler 加权蓄水池采样的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

流式加权抽样怎么单遍低内存？（spec 6039 /
effort #6039 / T39）

## Resolution

**WeightedReservoirSampler（core/policy，源码本轮入档）**：
A-Chao——前 m 项直入池，其后以 w_i/sumW 概率替换池中均匀
一项；种子化 Random 确定性；sample/size/seenCount/capacity
读数；capacity≤0/weight≤0 fail-fast。
