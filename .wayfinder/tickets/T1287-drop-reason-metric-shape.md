---
id: T1287
title: 丢弃计数 reason 维度指标的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 19 轮：`buzhou.eventbus.dropped` 无维度 counter 只给总量——按 reason 维度的指标序列（值域封闭 6 值）是否应扩散？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 19 轮 = effort #918 / spec 918 / impl 671）：扩散成立但须双轨并存。落点 `BufferedEventDispatcher.countDrop`：保留既有无维度总量 counter（`buzhou.eventbus.dropped`——既有面板序列不分裂），**新增**带 `reason` tag 的 counter（`buzhou.eventbus.dropped-reason`，值域 = 6 个固定字面量——drop-oldest/drop-oldest-race/block-timeout/interrupted/dispatcher-closed/closed-undelivered，编译期封闭有界，TagCardinalityGuard 兜底之上天然安全）。常量抽出（DROP_REASON_* static final——无魔法字符串）。EventDropBreakdown 的 reason 键与指标 tag 同源（同一常量集——读面与指标口径一致）。
