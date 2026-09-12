---
id: T1060
title: 限流头跨供应商归一解析的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

Anthropic 头名另一套——归一解析吗？混合来源允许吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 31 轮 = effort #730 / spec 730 / impl 630）：`parseFlexible`——先 OpenAI（非 empty 即用），缺项回退 Anthropic 名；**不混合来源**（并存时 OpenAI 优先）。utilization/pressure 语义与 719 一致。
