# Spec 10025 — HaltonSequence（effort #10025，X26）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10051–X10052，impl 2428）。
> 借鉴：Halton 序列（Halton 1964——SciPy qmc/渲染 QMC 同源）

## Problem Statement

无方向数字表需求的最简低差异序列——互素基数位逆序小数化即得。

## Solution

HaltonSequence（core/metrics，静态纯函数面）：coordinate(dimension,index)+sample——第 d 维取第 d 素数为基、radicalInverse 数位逆序小数化 ∈[0,1)；维数受素数表约束；维数/索引/样本数 fail-fast。

## Testing Decisions

逆根手锚（radicalInverse(1,2)=0.5/(3,3)=4/9 等）+值域+基 2 一维分层均匀+首点零+确定性+fail-fast 四面。

## Out of Scope

不做跳基/加空位变体（Scrambled/Revised 另立）；不做>8 维（素数表契约）；不做 Sobol 方向数字面（异面）。

## Further Notes

与 SobolSequence（10024）同域不同面：逆根数位重排 vs 方向数字异或。
