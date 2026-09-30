# Spec 9039 — SplitMix64 可分裂随机（effort #9039，W40）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9079–W9080，impl 2392）。
> 借鉴：SplitMix64（Steele 2014——JDK SplittableRandom 内核/Libc++ 同源）

## Problem Statement

LCG 相邻种子短程相关——**SplitMix64**：
黄金比例定序+最终化混合，种子分裂
即独立流。

## Solution

SplitMix64（core/metrics，静态纯函数面）：
nextState/mixOutput 分离+next 便利面；
GOLDEN_GAMMA 常量公开；状态 0 合法。

## Testing Decisions

标准常数金向量（Python 对拍锁定）；相邻
种子翻位 28..36 雪崩圣像；1000 分裂流
首步互异；确定性；fail-fast。

## Out of Scope

不做 SplitMix32/变体；不做流封装类；
不做密码学承诺；不做 PCG 组合（另立）。

## Further Notes

与 XorShift64（9038）同族不同面：
定序器 vs 推进器。开发勘误：记忆金向量
证伪（Python 对拍锁定）+nextState 零态
过度限制放开。Wave 7 第四件。
