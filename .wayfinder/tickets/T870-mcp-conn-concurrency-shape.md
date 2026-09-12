---
id: T870
title: MCP 每连接并发上限的执行语义裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

MCP server（尤其 stdio 单线程实现）对同一连接的并发调用敏感；buzhou 并行工具 fan-out 会把多个调用同时打到同一 server 连接。每连接并发上限应取什么执行语义？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 11 轮 = effort #600 / spec 610 / impl 463）：

1. 上限挂注册表 `setPerConnectionConcurrencyLimit(Integer)`（volatile，Entry 创建时读取装配信号量；null/<=0 = 不设——默认零行为变化）。
2. 执行语义 = **阻塞可中断获取**（虚拟线程便宜、core tool-timeout 兜底总时长；fail-fast 会把正常突发误杀）。中断 → 失败转文本（与摘除拒绝同词汇），不抛不炸工具循环。
3. 许可与既有引用计数正交：先引用（DRAINING 拒绝语义不变）再许可。
4. 不做协议级协商（server 声明并发能力）——客户端侧静态上限先行，协商留雾区。
