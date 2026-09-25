---
id: T6275
title: T 会话 T38 Stable Bloom Filter 稳定布隆过滤器的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

流式成员判定怎么自动遗忘旧事件？（spec 6037 /
effort #6037 / T38）

## Resolution

**StableBloomFilter（core/metrics，源码本轮入档）**：每次
插入前确定性游标衰减 d 位、再 k 哈希置满（饱和 3）——旧
成员停插即淡出；SplitMix64 游标+双哈希确定性；槽位/衰减/
哈希数越域与 null fail-fast。
