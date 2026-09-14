---
id: T2269
title: design-incompleteness 小缺口清扫（F7 + F10）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 10 轮：F 系缺口中 F7/F10 的处置口径？

## Resolution

**用户常设授权 AFK（可推翻）**

查重结论：F1（瞬断重试）未修留候选池（工程量大）；F2 部分修（ToolTimeoutOverrides 已消费超时键）；F5 已修（hit/miss/evicted 可读 API + spec 737）；F9（config-reference 文档）未修留独立轮；F7/F10 本轮清扫。

F7：canary.selected payload 补 sessionId（FallbackChain.EVENT_CANARY_SELECTED 的 Javadoc 自钉「sessionId + model」但 payload 只有 model/primary——多会话共用监听面无法定位归属）；null 会话上下文省略字段（Map.of 不容 null，LinkedHashMap + copyOf 条件包含）。

F10：spec 07 两处回写——`AgentSession.resume()` 推演名指向实现定名 `SessionInterrupts.resumeWith`（spec 12 面，功能等价）。
