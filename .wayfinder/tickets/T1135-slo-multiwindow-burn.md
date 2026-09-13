---
id: T1135
title: SLO 多窗燃烧率判定的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

双窗共振判定做进 ErrorBudget 还是独立判定脑？毛刺/渗漏区分语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 18 轮 = effort #817 / spec 817 / impl 570）：`SloMultiWindowBurn` 纯函数独立判定脑（ErrorBudget 零变更组合式）——双窗同超且样本足判 incident；独热带 reason 诊断（毛刺/渗漏）；≥ 边界；fail-fast。
