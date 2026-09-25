---
id: T6265
title: T 会话 T33 Extendible Hashing 可扩目录哈希的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

哈希扩容怎么免全量重哈希？（spec 6032 /
effort #6032 / T33）

## Resolution

**ExtendibleHashing（core/metrics，源码 T30 预载）**：哈希低
globalDepth 位寻址目录；桶溢出先翻倍目录再按 localDepth+1
分裂单桶；集合语义重复幂等；size/directorySize/globalDepth/
bucketCount 读数。
