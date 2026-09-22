---
id: T2957
title: 装箱平衡的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

批任务装箱数与浪费率怎么事前计算？（spec 1878 / effort #1878 / R79）

## Resolution`

**K8s/Mesos bin-packing 语义纯计算 `BinPackBalance`（core/policy）**：
pack（FFD 降序保序稳定 + 首个可容箱 + PackResult{binsUsed,loads}）+
wasteRatio（1−Σ载荷/(箱数×容量)，0 箱哨兵 0.0）。容量≥1/体积≥0
fail-fast。落轮前 grep 复核：TwoChoiceSelector（Q-3022 在线两随机）
占坑原选题，本静脉为批任务离线装箱，不撞。
