---
id: T1321
title: 事实衰减预报读法的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 27 轮：FactDecayPolicy（spec 604）有 decayed/injectable——「这个 fact 还剩几轮被过滤」的预报读法是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 27 轮 = effort #926 / spec 926 / impl 679（票号改号：T1299/T1300 复用冲突→T1321/T1322））：缺口成立——运维/宿主无法提前感知「哪批事实即将衰出注入」。落点 `FactDecayPolicy.turnsUntilFloor(double confidence)`：解析逆函数 `t = halfLifeTurns × log2(confidence / floor)` 向上取整（floor > 0 时）；confidence ≤ floor 返回 0（已衰出）；floor = 0 返回 Long.MAX_VALUE（永不过滤——floor=0 时 injectable 恒真语义一致）。校验 confidence ∈ (0,1]。纯函数零行为变化。
