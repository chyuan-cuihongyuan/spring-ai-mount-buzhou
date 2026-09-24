---
id: T6161
title: S 会话 S31 Jump Hash 跳跃一致哈希的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

分片路由怎么做到极简内存且扩容只搬最少的键？（spec 5030 /
effort #5030 / S31）

## Resolution

**JumpHash（core/policy）**：Google Lamping-Veach 论文思想——
bucketOf 线性同余逐桶随机游走，m→m+1 约 m/(m+1) 键留原桶；
FNV-1a 64 稳定指纹字符串入口；bucketCount≤0/null fail-fast。
