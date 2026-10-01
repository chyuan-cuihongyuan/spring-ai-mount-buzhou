# Spec 11021 — MatrixChainOrder 矩阵链区间 DP（effort #11021，Y22）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11043–Y11044，impl 2474）。
> 借鉴：CLRS 15.2 思想——区间 DP 同源

## Problem Statement

结合律代价最优化——区间 DP 的括号化面（Karger 随机化测试面勘误换静脉）。

## Solution

MatrixChainOrder（core/concurrent）：minMultiplications(int[])——维度数组长度 n+1 表 n 矩阵；区间长 2..n 逐层 m[i][j] 闭式递推；long 域。

## Testing Decisions

CLRS 手锚（p=[30,35,15,5,10,20,25]→15125）+双矩阵 p=[10,20,30]→6000+随机小链（n≤6 维 ≤20）与暴力递归枚举全切分圣像+确定性+fail-fast 五面。

## Out of Scope

不做括号方案重构输出（s 表消费另立）；不做并行化面。

## Further Notes

换静脉勘误入档（Karger 随机化域确定性测试面不稳——圣像 flake 风险）；Wave 4 图结构族第四件（区间 DP 面）。
