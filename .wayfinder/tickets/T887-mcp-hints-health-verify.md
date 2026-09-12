---
id: T887
title: 注解健康面验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T886
created: 2026-09-12
---

## Question

健康聚合如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（McpHealthHintsTest 2/2）：双 server 三工具两 destructive → selfReportedDestructiveToolCount=2 且 dangerousToolCount=1（分列）；空 hints 零计数；禁用路径 enabled=false。
