---
id: T6273
title: T 会话 T37 Counting Bloom Filter 计数布隆过滤器的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

布隆过滤器怎么支持删除？（spec 6036 /
effort #6036 / T37）

## Resolution

**CountingBloomFilter（core/metrics，源码本轮入档）**：k 哈希
计数数组——插入 +1、删除 −1 饱和于 0，contains 全计数 >0
（无假阴性可删除）；假阳性率参数化+SplitMix64 双哈希探针
确定性；参数越域/null fail-fast。
