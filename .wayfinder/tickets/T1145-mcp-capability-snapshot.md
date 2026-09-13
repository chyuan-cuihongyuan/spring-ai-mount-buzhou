---
id: T1145
title: MCP 能力协商快照的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

能力快照做单点还是并入 706 diff？seam 异常语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 23 轮 = effort #822 / spec 822 / impl 575）：`McpCapabilitySnapshot` 单点快照（706 的基线输入形状，正交不并）——三观察点采集+hint 计数+排序指纹；seam 异常逐路降级空真；null connection fail-fast。
