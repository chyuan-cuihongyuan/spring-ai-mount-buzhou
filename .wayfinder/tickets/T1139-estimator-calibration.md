---
id: T1139
title: Token 估算校准审计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

估算 vs 真值偏差如何量化？误差方向口径与近窗语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 20 轮 = effort #819 / spec 819 / impl 572）：`EstimatorCalibrationAudit`——相对误差 (est−act)/actual（正高估负低估）；累计均值+双向 bias 占比+近窗 128 P95；脏对忽略；事后审计不改估算器。换题注记：原 PII 置信分布不成立。
