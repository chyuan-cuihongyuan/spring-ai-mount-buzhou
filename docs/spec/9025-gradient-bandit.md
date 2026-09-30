# Spec 9025 — Gradient Bandit 梯度偏好选择（effort #9025，W26）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9051–W9052，impl 2378）。
> 借鉴：Gradient bandit（Sutton & Barto 2.8——softmax 偏好学习）

## Problem Statement

ε-greedy 不积累偏好、UCB 依赖均值估计——
**梯度 bandit**：偏好即策略，基线消尺度，
softmax 出概率。

## Solution

GradientBandit（core/experiment，静态纯函数面）：
probabilities（数值稳定 softmax）/select/update
（基线入参）。

## Testing Decisions

全零均匀锚；偏好差 ln3 比恰 3:1；平移
不变锚；高斯奖励 3 万轮分岔圣像；
确定性；fail-fast。

## Out of Scope

不做基线内置（调用方维护——纯函数面）；
不做非平稳衰减（α 衰减面）；不做上下文。

## Further Notes

与 Exp3Bandit（9024）同族不同面：
乘性 vs 加性。Wave 5 第二件。
