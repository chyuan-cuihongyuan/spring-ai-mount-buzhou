# Spec 2032 — UCB1 选择器（effort #2032，R33）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3165–T3166，impl 1583）。
> 借鉴：多臂老虎机 UCB1（Auer et al.）——置信上界平衡探索/利用。

## Problem Statement

均值贪心选臂（备模型 / 端点 / 策略变体）的已知病：早期不幸的臂被
永久冷落（估算未收敛即判死刑）——最优臂一次坏抽样后饿死；纯随机
探索又浪费容量。需要「不确定性内最优」的选择。

## Solution

`Ucb1Selector`（core/concurrent，synchronized 小临界区）：

- `registerArm(id)`（非空非重复 fail-fast）；`recordReward(id, reward
  ∈ [0,1])`（归一化由调用方）；
- `selectArm()`：未试臂优先（每臂至少一试——UCB1 标准）；否则
  UCB = mean + c×√(2 ln N / nᵢ) 最大者（c 默认 1.0；0 = 纯均值贪心
  显式退化）——尝试少的臂置信半径大自动被探索；
- 读数：armMeans() / armPulls()（探索覆盖对账——冷落臂显形）。

## User Stories

1. 作为路由作者，备模型探索不判死刑——坏抽样后仍小份额再试，真值
   显形后收敛最优。
2. 作为调参者，c 旋钮调探索强度；armPulls 分布直读探索覆盖。

## Testing Decisions

- 未试臂优先三臂各一试；200 轮 good 臂主导但 bad 仍有份额（exploit ×
  explore 并存）；不幸首抽不饿死（半径再探）；零探索退化贪心（饿死
  是显式选择）；空选 null；均值/计数追踪；畸形七型 fail-fast。

## Out of Scope

- 不做 Thompson 采样/上下文老虎机（奖励独立同分布口径）；不接备模型
  排序链（接线归后续轮）。

## Further Notes

- 与延迟感知备模型排序（#24 均值贪心）互补：那快而贪，这稳而全。
