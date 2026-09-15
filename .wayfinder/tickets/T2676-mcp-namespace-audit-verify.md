---
id: T2676
title: MCP 命名空间冲突普查的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2675
created: 2026-09-15
---

## Question

McpNamespaceAudit 怎么验证？（spec 1737 验收/裁决）

## Resolution

McpNamespaceAuditTest：冲突判定+占用集合/幂等登记/空态与匿名桶。
