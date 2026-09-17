---
id: T5045
title: Q 会话 R23 二择选择器的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

多目标派单怎么免中心表免全扫又压尾部过热？（spec 3022 / effort #3022 / R23）

## Resolution

**TwoChoiceSelector（core/concurrent，纯函数）**：Power of Two
Choices——随机抽两相异候选取负载小者（并列先抽），O(1) 决策换
最大负载 Θ(ln n)→Θ(ln ln n) 指数级改善。loads 快照归调用方，
本件零状态可回放。
