# Spec 10026 — LatinHypercube（effort #10026，X27）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10053–X10054，impl 2429）。
> 借鉴：拉丁超立方采样（McKay–Beckman–Conover 1979——SciPy qmc.LatinHypercube 同源）

## Problem Statement

纯随机采样边际层聚集——每维 n 层一格一点保证边际全覆盖的分层随机化。

## Solution

LatinHypercube（core/metrics，静态纯函数面）：sample(dimension,samples,seed)——每维 Fisher–Yates 层置换+层内均匀一点；种子驱动确定（同种子同采样/异种子异样）；维数/样本数 fail-fast。

## Testing Decisions

每层恰一点圣像（3 维 20 样本逐维核验）+值域+种子确定/异种子异样+fail-fast 三面。

## Out of Scope

不做正交数组强化（OA-LHS 另立）；不做条件/相关性控制面；不做确定序列面（Sobol/Halton 异面）。

## Further Notes

与 Sobol/Halton（10024/10025）同域不同面：种子随机化 vs 确定序列。
