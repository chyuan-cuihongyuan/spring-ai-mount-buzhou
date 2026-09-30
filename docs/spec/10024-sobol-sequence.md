# Spec 10024 — SobolSequence（effort #10024，X25）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10049–X10050，impl 2427）。
> 借鉴：Sobol 序列（Sobol 1967——SciPy qmc/图形学 QMC 同源）

## Problem Statement

蒙特卡洛误差 O(1/√N) 收敛慢——准蒙特卡洛 (t,m,s)-网低差异序列 O((log N)^s/N)。

## Solution

SobolSequence（core/metrics，静态纯函数面）：coordinate(dimension,index)+sample——每维方向数字 new-Joe-Kuo 首八维表、Gray 码 n⊕(n≫1) 选位异或得点/2³²；维数/索引/样本数越域 fail-fast；无符号掩码口径。

## Testing Decisions

单位超立方值域+首点零+一维 32 点 8 区间分层均匀圣像+二维 16 点 4×4 格每格 1 点分层+确定性 deepEquals+fail-fast 四面。

## Out of Scope

不做随机化打乱（Owen scrambling 另立）；不做>8 维（表行数契约）；不做跳位/跳跃面。

## Further Notes

与 HaltonSequence（10025）同域不同面：方向数字异或 vs 逆根数位重排。
