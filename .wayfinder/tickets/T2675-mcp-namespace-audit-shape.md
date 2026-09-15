---
id: T2675
title: MCP 命名空间冲突普查的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

McpNamespaceAudit 的形状怎么裁决？（spec 1737 / effort #1737 / R38）（spec 1737 验收/裁决）

## Resolution

实例面 synchronized register(server, tool)（缺名归 _anonymous_）+census(tools/collidingTools/maxServersPerTool)+serversOf(tool) 占用集合（重复登记幂等）——npm scope 冲突思想，装配期告警优于运行期歧义。
