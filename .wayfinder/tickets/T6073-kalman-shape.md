---
id: T6073
title: R 会话 R37 标量卡尔曼滤波的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

单指标在线估计怎么带不确定性闭环融合？（spec 4036 /
effort #4036 / R37）

## Resolution

**ScalarKalmanFilter（core/metrics）**：标量卡尔曼——预测
（方差增长 p+=q）+ 更新（增益 k=p/(p+r) 融合收缩）；
增益随确定性自适应（update 降、predict 回升）；读数
state/variance/lastGain；畸形 fail-fast。
