---
id: T1319
title: gate 阈值漂移读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 38 轮：gate 历史（spec 914）有判定序列——「阈值是否被频繁调整」（配置不稳定信号）的聚合读面是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 38 轮 = effort #938 / spec 938 / impl 690）：缺口成立——阈值反复调整（CI 红了就调阈值）是流程不健康的信号，需要显形。落点 `EvalGate.thresholdDrift(List<GateDecision>)` 静态纯函数：相邻判定 threshold 不同的次数 = 调整次数 + `record ThresholdDrift(int transitions, int sampled)`（sampled = 参与比较的判定对数 = size−1）。单条历史 0 次调整；空/单条返回 0。纯函数零行为变化（914 历史面的聚合视图）。
