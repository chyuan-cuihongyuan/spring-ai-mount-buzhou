# Spec 11001 — VanDerCorput 逆根序列（effort #11001，Y2）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11003–Y11004，impl 2454）。
> 借鉴：van der Corput 1935 思想——SciPy qmc 同源（HaltonSequence 已占异面：一维基座 vs 多维合成）

## Problem Statement

一维低差异序列——逆根数位重排的最简基座面。

## Solution

VanDerCorput（core/metrics）：sequence(int base, int count)——第 i 点=把 i 的 base 进制数位逆序置小数点后；长整型域逐位取除（无幂表依赖）。

## Testing Decisions

基 2 首 8 手锚（0,1/2,1/4,3/4,1/8,5/8,3/8,7/8）+8 区间各一点分层圣像+基 3 手锚（1/3,2/3,1/9）+值域 [0,1)+确定性+fail-fast 五面。

## Out of Scope

不做随机化偏移面（scrambling 另立）；不做多维合成（HaltonSequence 已占）。

## Further Notes

与 HaltonSequence（10025）同域不同面：一维基座 vs 多维互素基合成；Wave 1 变换族首件。
