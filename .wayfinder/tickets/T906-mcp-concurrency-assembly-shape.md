---
id: T906
title: MCP 每连接并发上限 yml 装配的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

spec 610 的每连接并发上限只有编程 setter——stdio 单线程 server 防护要三行 yml。装配怎么开？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 29 轮 = effort #600 / spec 628 / impl 481）：

1. `buzhou.mcp.per-connection-concurrency-limit`（缺省 null = 不设零变化；<=0 装配 fail-fast）→ BuzhouMcpProperties 扩组件 → McpModule.Builder 透传 → 注册表 setter（Entry 创建时装配信号量，既有条目不追溯——610 语义）。
2. 三参兼容构造保留；canonical @ConstructorBinding（多构造绑定坑 R39 同法——本会话第三次撞上：缺注解 binder 报 No default constructor，全链路 bean 塌）。
