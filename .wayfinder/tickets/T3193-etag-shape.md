---
id: T3193
title: ETag 条件请求匹配的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

查询/导出端点的版本复用与乐观并发怎么原语化？（spec 2046 / effort #2046 / R47）

## Resolution

**HTTP RFC 7232 纯函数匹配器 `EntityTagMatcher`（core/webhook）**：
强比较（全等非弱——If-Match 字节等价门）+弱比较（剥 W/ 前缀——
If-None-Match 语义等价门）+ifNoneMatchHit（* 通配/列表任一弱等 →
304 省载荷）+ifMatchSatisfied（* 通配/列表任一强等 → 满足，否则 412
乐观并发防护）+无头恒 false（If-Match 缺失不做检查——调用方区分）。
