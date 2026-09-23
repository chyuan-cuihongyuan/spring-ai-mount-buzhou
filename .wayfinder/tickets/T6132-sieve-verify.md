---
id: T6132
title: S 会话 S16 SIEVE 缓存驱逐的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6131]
created: 2026-09-24
---

## Question

S16 合同怎么逐一验绿？（spec 5015 / effort #5015 / S16）

## Resolution

**验证通过**：SieveCacheTest 五测全绿——命中置位不重排；
清位跳过一次后驱逐；分叉场景（最老已访问项跨插入存活）；
容量 1 边界；capacity≤0/null fail-fast；确定性回放。
