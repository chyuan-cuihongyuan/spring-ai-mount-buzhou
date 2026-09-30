# Spec 9032 — Fast Inverse Sqrt 平方根倒数速算（effort #9032，W33）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9065–W9066，impl 2385）。
> 借鉴：Fast inverse sqrt（Quake III 1999 Q_rsqrt 魔数 0x5f3759df 同源——游戏图形学传奇）

## Problem Statement

归一化/L2 预除热路径上 Math.sqrt 求逆
过度精确——**位型魔数初值+一次牛顿**：
<0.2% 相对误差的速算。

## Solution

FastInverseSqrt（core/metrics，静态纯函数面）：
inverseSqrt(float)——0x5f3759df 位级初值+
一次牛顿；误差契约 <0.2%；有限正数域。

## Testing Decisions

对数刻度 1e-6..1e6 万点误差契约圣像；
4/9/1e10 锚值（0.2% 契约内）；确定性；
fail-fast（含 ∞ 拒绝——位级初值无意义）。

## Out of Scope

不做双迭代（更高精度变体）；不做
double 域（float 位型明示）；不做与
Math 精确面竞争（历史面诚实边界）。

## Further Notes

与 KahanSummator 同域不同面。
Wave 6 第三件。
