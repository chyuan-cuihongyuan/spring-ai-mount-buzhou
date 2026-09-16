---
id: T3165
title: UCB1 选择器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

选臂的探索/利用平衡怎么原语化？（spec 2032 / effort #2032 / R33）

## Resolution

**多臂老虎机 UCB1 线程安全选择器 `Ucb1Selector`（core/concurrent）**：
registerArm+recordReward（[0,1] 归一）+selectArm（未试臂优先——每臂
至少一试；否则 UCB=mean+c×√(2lnN/nᵢ) 最大）——尝试少半径大自动探索，
真值显形后收敛最优；c=0 显式退化纯贪心；armMeans/armPulls 探索覆盖
对账面。
