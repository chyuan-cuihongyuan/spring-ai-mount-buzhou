---
id: T1174
title: 半开探测成功率读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1173]
created: 2026-09-13
---

## Question

计数/streak/近窗滑动如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 37 轮 = effort #836）：HalfOpenProbeStatsTest 4 例——0.6 近窗率+streak 清零/streak=2→归零/滑动后率 1.0 累计保留/封顶+超封顶 null+null 忽略。
