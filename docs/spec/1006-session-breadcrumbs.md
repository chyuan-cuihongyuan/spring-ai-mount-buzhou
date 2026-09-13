# 1006 — 会话面包屑环形读面

> 来源：J 会话第 7 轮 = effort #1006（[T1463](../../.wayfinder/tickets/T1463-breadcrumbs-shape.md) / [T1464](../../.wayfinder/tickets/T1464-breadcrumbs-verify.md) / impl 759）。借鉴：Sentry [breadcrumbs](https://docs.sentry.io/platforms/java/enriching-events/breadcrumbs/)——事件时间线尾部环，「出事后看最后发生了什么」。

## Problem Statement

会话事件面已有聚合统计（spec 13 EventBusStats 计数、spec 900 丢弃分类），但**时间线尾部**缺失：会话异常、租约失效或硬关后，「最后发生了什么事件、按什么顺序」不可回溯——全量事件存储过重（observability store 面向 span），轻量尾部时间线是排障刚需。

## 目标

- 新公共 record `EventBreadcrumb(long epochMillis, String type)`（core.session，api 面）：只记类型与时刻，**不记 payload**（事件载荷可能含会话内容——敏感红线，与 ToolSlowLog 只记名不记参同纪律）。
- 内部 `BreadcrumbRing`（core.internal.session，包私有）：有界环 32 新→旧，`record(type)` / `snapshot()` / `clear()`。
- 记录点 = `DefaultAgentSession.deliverEvent`：SYNC 内联（默认）与 buffered 分发回调的**共同漏斗**，一行 record 覆盖双模式。
- 读面 = `AgentSession.breadcrumbs()`：default 空表（其他实现零负担）+ DefaultAgentSession 覆写返回新→旧不可变快照。

## 兼容性

纯增量读面：分发语义零变化（deliverEvent 首行多一次同步环操作）；接口新增 default 方法不破坏既有实现；无新配置项。

## Out of Scope

- payload 摘要/尺寸分档入面包屑（EventPayloadSizeAudit 已有尺寸审计——如需并入另行立项）。
- 跨会话聚合视图（面包屑是单会话现场，聚合属 observability store 域）。
