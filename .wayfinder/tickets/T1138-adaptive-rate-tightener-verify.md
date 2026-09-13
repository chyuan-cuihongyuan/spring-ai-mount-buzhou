---
id: T1138
title: 限流自适应收紧器验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1137]
created: 2026-09-13
---

## Question

收缩下限/保持/步进恢复边界如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 19 轮 = effort #818）：AdaptiveRateTightenerTest 6 例——0.5 收缩+保持窗+步进边界（2001→0.5、2500→1.0）/10 次 429 收缩至 0.05 下限+确定性/0.25→0.5→1.0 多步精确/模型独立+null/封顶 truncated/五参 fail-fast。首跑恢复步起算点笔误修正（步从保持窗结束起算）。
