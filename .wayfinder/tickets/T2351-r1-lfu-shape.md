---
id: T2351
title: R1 语义缓存 LFU 采样驱逐的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2350
created: 2026-09-15
---

## Question

N 会话第 1 轮：驱逐策略升级选哪种借鉴形状——TinyLFU 频率准入（admission）还是 Redis LFU 采样驱逐（eviction sampling）？

## Resolution

选 **Redis allkeys-lfu + maxmemory-samples 采样驱逐**。理由：语义缓存的 key 不可精确哈希
（重访是相似度意义上的，措辞变化即换 embedding），TinyLFU/Caffeine 式频率 sketch 准入无锚点；
而「驱逐时在 LRU 序前 N 个候选中淘汰命中计数最低者」只依赖条目自身命中计数，完全适配。
简化决策：线性计数 + 1000 封顶（Redis 的概率对数递增 + 衰减对本量级过度设计，TTL 天然兜底）；
新条目计数 0（写入不代表查询价值）。opt-in `evictionSampleSize` 默认 0 = 纯 eldest LRU 零变化。
