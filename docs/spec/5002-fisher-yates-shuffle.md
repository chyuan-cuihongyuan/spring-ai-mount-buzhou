# Spec 5002 — Fisher-Yates 无偏洗牌（effort #5002，S3）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6105–T6106，impl 2153）。
> 借鉴：Fisher–Yates/Durstenfeld 无偏洗牌（Knuth shuffle）。

## Problem Statement

顺序随机化的病：排序键取模（经典有偏洗牌——排列分布不均）、
每次全量重建（O(n²)）——**O(n) 无偏原地面**缺失。

## Solution

`FisherYatesShuffle`（core/policy）：

- Durstenfeld 变体：从尾向前，每步与 `[0, i]` 均匀取 j 交换
 （`nextInt(i+1)` 含自身——无偏的关键；取 `[0, i)` 即有偏）；
- O(n) 原地 + 不可变变体（返回新列表）+ 索引排列变体
 （不触碰元素只产排列——确定性种子注入）；
- 均匀性：n! 排列等概率；
- fail-fast：null 列表 / 负 size。

## User Stories

1. 作为采样/评估作者，对照组划分无顺序偏差。
2. 作为审计作者，同种子同排列（确定性可回放）。

## Testing Decisions

- 均匀性：n=3 六排列 × 6000 种子频次各 ∈ 期望 ±25%；双射
 （索引排列排序=恒等）；同种子同排列 / 同种子重放相等；
 null/负 size fail-fast。

## Out of Scope

- 不做加密安全随机（RandomGenerator 注入口径）；不做部分
  洗牌（partial shuffle）；不做流式洗牌（归蓄水池抽样族）。

## Further Notes

- 与 DeterministicHash（确定性散列公共件）同族不同面：散列
  位置映射 vs 排列均匀化。Wave 1 第三件。
- 里程碑：S3/50（6%）。
