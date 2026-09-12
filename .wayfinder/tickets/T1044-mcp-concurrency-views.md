---
id: T1044
title: MCP 每连接并发占用视图的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

610 并发闸是黑盒——加占用读数吗？接口面还是实现面？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 23 轮 = effort #722 / spec 722 / impl 622）：`McpConcurrencyView(server,limit,available,inFlight)` 公共 record（limit=-1 哨兵=未设）+接口 default `concurrencyViews()`（实现未支持零面）+DefaultMcpClientRegistry 覆写（Entry 存 limit 原值+availablePermits+inFlight）。快照口径；队列长度不做（JDK Semaphore 不暴露）。
