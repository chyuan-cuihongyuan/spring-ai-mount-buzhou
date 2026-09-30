# Spec 10028 — SliceSampler（effort #10028，X29）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10057–X10058，impl 2431）。
> 借鉴：切片采样（Neal 2003——PyMC/NumPyro 同源；MetropolisHastings 已占异面）

## Problem Statement

MCMC 免调提议分布——辅助竖切+步出收缩逼近条件分布，无接受率调参面。

## Solution

SliceSampler（core/metrics，静态纯函数面）：sample(logDensity,initial,count,stepWidth,seed)——切片高度 y=ln f−Exp(1)、步出扩区（定宽+上限 32 步护栏）+收缩拒绝逼近；对数密度接口防下溢；null 密度/非正步宽/计数越域/初始密度非正 fail-fast。

## Testing Decisions

标准正态矩圣像（2 万样本 mean±0.1/var±0.2）+有界均匀域均值与界内核验+确定性+fail-fast 四面。

## Out of Scope

不做多元坐标轮换面（单变量原语）；不做超束/仿射变体（Neal 论文变体另立）；不做密度梯度面。

## Further Notes

与 MetropolisHastings（已占）同域不同面：无提议分布 vs 接受率调参。
