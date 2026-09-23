---
id: T6131
title: S 会话 S16 SIEVE 缓存驱逐的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

缓存驱逐怎么命中零重排且已保护项跨插入存活？（spec 5015 /
effort #5015 / S16）

## Resolution

**SieveCache（core/cache）**：SIEVE 思想——插入序 FIFO 环 +
访问位，命中只置位不重排（lazy promotion）；驱逐指针清位跳过
/ 零位摘除（一次保护机会，停在驱逐点后）；与 LRU 分叉显证。
