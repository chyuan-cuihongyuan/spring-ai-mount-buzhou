---
id: T6098
title: R 会话 R49 Cache-Control 指令裁决的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6097]
created: 2026-09-24
---

## Question

R49 合同怎么逐一验绿？（spec 4048 / effort #4048 / R49）

## Resolution

**验证通过**：CacheControlDirectivesTest 六测全绿——解析
（大小写/引号/未知记录/负值 fail-fast）；裁决四象限 + 共享
缓存 s-maxage 优先 + 确定性回放。
