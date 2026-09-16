---
id: T3108
title: 记忆强度三分量评分的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3107]
created: 2026-09-17
---

## Question

MemoryStrengthScore 合同（半衰期/饱和/钳制/单调/畸形）怎么钉住？（spec 2003 / effort #2003 / R4）

## Resolution

**七用例全绿**（首跑 1 红根因：e 指数口径 Δt=T 时 0.368 ≠ 半衰期语义
0.5——改 2^(−Δt/T) 后 7/7）：半衰期三点 1/0.5/0.25 / 对数饱和边际递减
（10k→1M 增益 < 0→10）/ 重要度钳制三态（0.7 直通、5→1、−3→0）/ Δt
单调 / 输出域含 Long.MAX_VALUE 极值 / 畸形六型 fail-fast。
