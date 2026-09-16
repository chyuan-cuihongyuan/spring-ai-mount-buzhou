---
id: T3156
title: EWMA 估计器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3155]
created: 2026-09-17
---

## Question

EwmaEstimator 合同（锚定/递推/两极/收敛/单调/畸形）怎么钉住？（spec 2027 / effort #2027 / R28）

## Resolution

**八用例全绿**（首跑 1 红：50 期收敛容差 1e-6 过紧——(1−α)^50 残余
0.8% 数学口径，放宽 0.01 后 8/8）：首样本锚定非 NaN 判 / α=0.5 二
样本恰 150 / α=1 直通最新 / α=0.1 尖峰稀释（1000 尖峰后 190） / 50
期收敛 / 单调输入估计单调 / reset 回未锚定再锚定 / 畸形四型
（α=0、1.5、NaN、Inf）fail-fast。
