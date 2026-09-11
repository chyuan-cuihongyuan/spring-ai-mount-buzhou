---
id: T854
title: fork 谱系的落点裁决（OTel span-links 思想在 buzhou 模型里的诚实映射）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

OTel span links 用于跨 trace 关联（fork 子会话 ↔ 源会话）。buzhou 的 SpanContext 只有 (spanId, sessionId, turnSeq)、core 主链路不开 span（span 属 observability 管线）——span-links 思想应落在哪一层？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 3 轮 = effort #600 / spec 602 / impl 455）：

1. **落 state + 事件，不造 span 谱系**：fork 后向子会话写一条谱系 state `buzhou.fork.source`（value=源会话 id，producer=`buzhou.core.fork`，无 TTL）——任何模块（guard/spill/dashboard）经 sessionStateStore 可查「本会话 fork 自谁」；导出/导入天然携带。
2. 不复制任何源 state（预算重置语义不变），只写这一条新条目；普通 fork 与时间旅行 fork（forkFromTurn）两个入口都写。
3. `session.forked` 事件增强：`copiedMessages`（复制条数）+ `copiedSummary`（是否带摘要；时间旅行 fork 恒 false——未来泄漏防护）+（时间旅行）`upToTurn` 与前缀消息数。
4. otel 侧真 span links（跨会话 spanId 关联）不做——观测管线无 span 谱系模型，强行加是伪 link；留雾区。
