---
id: T920
title: 变换 fail-open 可观测的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

spec 169 的 fail-open（变换异常/null/空白→原文）完全静默——变换常年失效（字段笔误/上游格式变化）= 每次都拿原文省得少了，但「从未生效」与「从未失败」不可区分。怎么补？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 36 轮 = effort #600 / spec 635 / impl 488）：

1. `failOpenCount()` 编程面 + `buzhou.tool.transform-fail-open` 计数（tag: tool 有界）+ 首次失败 WARN（此后同类只计数不刷屏）。
2. 空原文短路不算失败（无变换机会）；成功路径零开销（不进计数路径）。
3. 不改 fail-open 语义（绝不丢数据的契约不变——只加可见性）。
