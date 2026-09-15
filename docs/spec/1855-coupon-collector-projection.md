# Spec 1855 — 收藏家覆盖期望（effort #1855，R56）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2911–T2912，impl 1456）。借鉴：
> 概率论 coupon collector——均匀抽样见全 k 类期望 k·H(k) 轮（长尾在最后
> 一类：收齐 10 类期望 ~29 轮而非 10 轮）。

## Problem Statement`

覆盖型测试（评测集全类见过/技能目录全技能触达）的轮数预算靠拍：以为
跑 k 轮就能见全 k 类——均匀抽样下期望是 k·H(k)（调和级数长尾），「还差
多少轮收齐」随进度可读的期望面缺位。

## Solution

`CouponCollectorProjection`（core/eval，静态纯函数）：

- `expectedDraws(totalKinds)` = k × H(k)（0 类 → 0）；
- `expectedRemaining(distinctSeen, totalKinds)` = k × (H(k) − H(s))
 （0 ≤ seen ≤ k 契约）；
- double 全程（防 k! 型溢出）。

## User Stories

1. 作为覆盖测试作者，10 类目录期望 ~29.3 轮——预算 30 轮有依据，不是
   拍 10 轮然后奇怪为何没见全。
2. 作为进度读者，已见 5/10 → 余期望直接读（长尾感知：后半比前半贵）。
3. 作为框架宿主，类口径（标签/技能/等价类）自声明，纯投影零采样。

## Implementation Decisions

- 纯投影不采样；调和级数显式循环（朴素——精度对预算场景足够）。

## Testing Decisions

- 已知小值（1/3/5.5）；长尾性质（k=10 ∈ (29,30)）；余轮单调递减+端点；
  畸形两型 fail-fast。

## Out of Scope

- 不做非均匀分布（加权 coupon 归未来静脉）；不接采样执行。

## Further Notes

- 与 EvalCoverageMatrix 互补：那是覆盖现状矩阵，这是收齐期望轮数。
