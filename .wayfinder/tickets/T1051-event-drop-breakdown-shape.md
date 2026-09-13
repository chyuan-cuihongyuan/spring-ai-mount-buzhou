---
id: T1051
title: 事件丢弃按原因分类读面的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by:
created: 2026-09-13
---

## Question

H 会话第 1 轮：事件丢弃可见性已有总量（EventBusStats.dropped / spec 13 §core-4）——按丢弃原因分类的读面是否有缺口？形态如何裁决？（Sentry discarded events：按 reason×category 分维可查询。）

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 1 轮 = effort #800 / spec 800 / impl 553）：缺口成立——dropped 总量已有，但 DROP_OLDEST 挤掉 / block-timeout / closed-undelivered / dispatcher-closed / interrupted 各原因只有 WARN 文本、无结构化读面。落点 `BufferedEventDispatcher`（事件丢弃的唯一现场）：countDrop 维护 `ConcurrentHashMap<String,LongAdder>` reason→count；新公共 record `EventDropBreakdown`（core.session，不可变快照 + forReason/total 便捷访问）经 `AgentSession.eventDropBreakdown()` default 方法暴露（与 eventBusStats 同构，SYNC 默认 empty，不破坏既有实现）。EventBusStats 原样不动（避免破坏快照兼容）。守恒不变量：breakdown 值之和恒等于 stats().dropped()。
