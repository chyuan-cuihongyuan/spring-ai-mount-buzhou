---
id: T6180
title: S 会话 S40 Segmented LRU 分段缓存的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6179]
created: 2026-09-25
---

## Question

S40 合同怎么逐一验绿？（spec 5039 / effort #5039 / S40）

## Resolution

**验证通过**：SlruCacheTest 六测全绿——试用头淘汰；晋升/
降级/淘汰逐段钉住；扫描不污染（a,b 存活+evicted=4）；upsert
覆盖晋升；透视不晋升；畸形 fail-fast。（晋升未摘试用键的
回归 bug 由分段读数钉住修复。）
