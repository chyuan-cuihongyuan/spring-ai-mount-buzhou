---
id: T1025
title: 工具侧滚动 max 同构扩散的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

HookTiming 已有滚动 max（spec 708）——工具侧 ToolTimingAggregator 生命周期 max 同样永不衰减。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 38 轮 = effort #738 / spec 738 / impl 540）：ToolTimingAggregator.Timing 增 windowedMax（RollingMaxCounter 复用——零新逻辑）+ windowedMax() 快照；ToolTimingHealth 行增 rollingMaxMicros。spec 708 同构扩散（组件复用即扩散——无需新抽象）。
