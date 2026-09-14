---
id: T2189
title: 评估通过率趋势审计（EvalPassRateTrend）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 44 轮：跨 run 通过率趋势审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：门判定（瞬时）/GateDecision 环形史（I T1279）均有——跨 run 趋势方向无审计面。

形状裁决：EvalPassRateTrend 纯函数（core/eval）——analyze(List<Double>)→TrendReport（Theil–Sen 成对斜率中位数——单点抗噪+Direction 闭集 INSUFFICIENT/DEGRADING/STABLE/IMPROVING 死区 ε=0.005/run）+passRates 保序回读；纯函数不触门。

Out of scope：显著性检验；多维拆分；回滚联动。
