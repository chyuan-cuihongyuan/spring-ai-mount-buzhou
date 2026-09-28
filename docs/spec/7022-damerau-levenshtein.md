# Spec 7022 — DamerauLevenshtein 距离（effort #7022，U23）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7245–U7246，impl 2274）。
> 借鉴：Lowrance & Wagner 1975（Damerau 1964 变体）思想。

## Problem Statement

手滑换位纠错的病：Levenshtein 三算子把相邻换位计 3
步（teh/the 真实编辑 1 步被高估）——**第四算子「相邻
换位」面**缺失。

## Solution

`DamerauLevenshtein`（core/metrics，静态工具面）：OSA
限制版四算子 DP（全矩阵 O(mn)——限制语义明示：ca→abc
=3 而真 DL=2，诚实边界入档）；similarity 归一比；
null fail-fast（空串=对侧长度）。

## Testing Decisions

- 换位经典（teh/the、ca/abc OSA 语义钉住）；空串/恒等；
  随机域界（|Δlen| ≤ d ≤ max）；fail-fast。

## Out of Scope

- 不做无限制真 DL（带全行历史变体）；不做编辑脚本还原。

## Further Notes

- 与 TextDistance（2052 Levenshtein）同族不同面：三算子
  vs 四算子（+相邻换位）。
- 里程碑：U23/50（46%）。
