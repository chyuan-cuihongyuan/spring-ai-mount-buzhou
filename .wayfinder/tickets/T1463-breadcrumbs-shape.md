---
id: T1463
title: 会话面包屑环形读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 7 轮：会话面包屑环形读面（Sentry breadcrumbs）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 7 轮 = effort #1006 / spec 1006 / impl 759）：缺口成立——事件面已有聚合统计（EventBusStats 计数、EventDropBreakdown 丢弃分类），但**时间线尾部**缺失：会话异常/终止后「最后发生了什么事件、按什么顺序」不可回溯。落点：新公共 record `EventBreadcrumb(epochMillis, type)`（core.session——只记类型与时刻，**不记 payload**，敏感红线同 ToolSlowLog）+ 内部 `BreadcrumbRing`（有界环 32 新→旧，core.internal.session 包私有直测）；记录点 = `DefaultAgentSession.deliverEvent` 双模式共同漏斗（SYNC 内联与 buffered 分发回调都经此，一行 record）；读面 = `AgentSession.breadcrumbs()` default 空表 + DefaultAgentSession 覆写（eventBusStats default 惯例同款）。
