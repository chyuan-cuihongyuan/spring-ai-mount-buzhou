---
id: T873
title: 维度漂移验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T872
created: 2026-09-12
---

## Question

漂移可见性如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SemanticCacheDimensionDriftTest 3/3）：

- 4 维条目 + 3 维查询：miss + 计数 1，重复查询累计 2，不抛。
- 同维度命中不受影响、计数恒 0。
- 漂移后写入新维度条目：新查询命中（自然收敛——清缓存非必需）。
