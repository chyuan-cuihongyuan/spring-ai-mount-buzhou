---
id: T5043
title: Q 会话 R22 Holt 预测器的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

趋势型序列怎么在线短程外推？（spec 3021 / effort #3021 / R22）

## Resolution

**HoltForecaster（core/metrics）**：Holt 双参数——水平 α + 趋势 β
双分量递推，forecast(h)=level+h·trend（EWMA 无趋势外推系统性滞后
病的根治）；首观测初始化+空态 NaN+参数开区间校验。季节性/阻尼
趋势留白。
