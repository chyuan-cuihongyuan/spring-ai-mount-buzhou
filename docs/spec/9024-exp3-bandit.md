# Spec 9024 — EXP3 Bandit 对抗多臂选择（effort #9024，W25）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9049–W9050，impl 2377）。
> 借鉴：EXP3（Auer 2002——adversarial bandit 无遗憾界经典）

## Problem Statement

随机 bandit 假设被对手操纵即崩——**EXP3**：
无分布假设，混合探索+指数权重，遗憾界
O(√(TKlnK))。

## Solution

Exp3Bandit（core/experiment，静态纯函数面）：
select(weights,γ,random)/update(chosen,reward)
原地/probabilityOf；奖励 [0,1]、γ∈(0,1]。

## Testing Decisions

混合分布手锚（0.25/0.86 逐值）；固定奖励
对手 2 万轮选中率>0.6；同种子回放；
fail-fast 七面。

## Out of Scope

不做 EXP3.X 显式探索变体；不做对手建模
（follow-the-regularized-leader 面）；不做上下文
bandit。

## Further Notes

与 Ucb1（2032）/Thompson（8024）同域不同面。
Wave 5 第一件。
