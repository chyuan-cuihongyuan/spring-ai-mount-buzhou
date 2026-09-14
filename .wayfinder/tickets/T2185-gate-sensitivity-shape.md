---
id: T2185
title: 门阈值敏感性扫描（GateThresholdSensitivity）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 42 轮：门阈值稳健性扫描面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：EvalGate 有环形史（I T1279）/多窗判定——阈值移动敏感度无扫描面。

形状裁决：GateThresholdSensitivity 纯函数（core/eval）——analyze(scores, threshold, δ)→SensitivityReport（δ 带左闭右开+tighten/loosen 翻转分向+sensitivityRatio 派生 -1 哨兵）+δ 负值 fail-fast；离线扫描不触门判定。

Out of scope：阈值寻优；曲线化；门历史联动。
