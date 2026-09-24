---
id: T6179
title: S 会话 S40 Segmented LRU 分段缓存的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

缓存怎么扛住一次性扫描不丢热数据？（spec 5039 /
effort #5039 / S40）

## Resolution

**SlruCache<K,V>（core/cache）**：PostgreSQL/Caffeine SLRU
思想——试用期/保护期双段各自 LRU；新键入试用尾、命中晋升
保护尾、保护满降级保护头回试用尾、淘汰只走试用头；透视
containsKey 不晋升；双段+evictedCount 读数；畸形 fail-fast。
