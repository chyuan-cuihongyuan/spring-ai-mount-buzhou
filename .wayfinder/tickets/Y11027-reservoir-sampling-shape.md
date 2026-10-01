---
id: Y11027
title: Y 会话 14 ReservoirSampling 水塘抽样 的形状裁决
type: task
status: closed
assignee: zcode-y
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

ReservoirSampling（core/metrics，静态纯函数面）：sample(populationSize,sampleSize,seed)——Algorithm R（前 k 满塘+i 入选概率 k/i 替换）均匀 k 样本升序返回；种子驱动确定；k>n/k<1 fail-fast。
