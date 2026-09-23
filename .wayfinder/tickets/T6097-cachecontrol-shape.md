---
id: T6097
title: R 会话 R49 Cache-Control 指令裁决的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

响应缓存怎么按标准指令语义裁决不被自造 TTL 覆盖？（spec 4048 /
effort #4048 / R49）

## Resolution

**CacheControlDirectives（core/policy）**：RFC 9111 §5.2——
解析（大小写不敏感/引号值/未知忽略但记录/负值 fail-fast）+
优先级裁决（no-store > no-cache > 新鲜度上限；共享缓存
s-maxage 压过 max-age）；嵌套 Directives 不另立面。
