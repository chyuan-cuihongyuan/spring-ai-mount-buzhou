---
id: T2853
title: 会话布隆粗筛的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

「从未见过」的快路径判定怎么零索引开销？（spec 1826 / effort #1826 / R27）

## Resolution

**Bloom filter 思想 `SessionBloomFilter`（core/session，synchronized）**：
add 幂等 + mightContain 粗筛（false=一定没见过，零假阴性契约；true=可能
见过）+ 确定性哈希（seed 混合 FNV 式扩散无随机数可回放）+ fillRatio 饱和度
（超 0.5 建议重建，布隆只增不能清）；4096 位×3 哈希默认常量；bits≥64、
hashes∈[1,8]、空白 id fail-fast。

