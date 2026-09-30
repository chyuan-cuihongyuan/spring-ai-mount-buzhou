# Spec 9044 — Simulated Annealing 模拟退火（effort #9044，W45）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9089–W9090，impl 2397）。
> 借鉴：Simulated annealing（Kirkpatrick 1983——VLSI 布局/NP-hard 启发式同源）

## Problem Statement

纯贪心陷局部最优、随机重启无结构——
**退火**：劣解按温度概率接受，
降温收敛+best-ever 记忆。

## Solution

SimulatedAnnealing（core/policy，静态纯
函数面）：minimize(energy,start,step,T0,
Tend,iters,random)；几何降温+高斯扰动。

## Testing Decisions

Rastrigin-lite 多凹坑跨坑圣像；多维
二次碗面；best-ever 不劣；同种子
全等；fail-fast 四面。

## Out of Scope

不做约束域（无界提议明示）；不做
自适应步长；不做并行回火（另立）；
不做组合问题专用邻域（归 TspTwoOpt）。

## Further Notes

与 MetropolisHastings 同根不同面：
采样 vs 最优化。Wave 8 第三件。
