---
id: T2266
title: MCP 危险工具默认动词模式的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2265
created: 2026-09-15
---

## Question

M 会话第 8 轮：默认动词模式如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-mcp -am test` 绿——
① 属性面：缺省构造 → 七动词默认集；显式空 List → 空（逃生门）；显式集透传不注入默认；
② 注册表端到端：默认模式集下 delete_records/exec_sql/send_email 命中、read_query/list_items 不误伤；
③ 既有 McpRealProtocolTest（显式模式）零回归。
