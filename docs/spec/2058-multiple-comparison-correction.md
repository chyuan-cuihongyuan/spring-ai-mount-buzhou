# Spec 2058 — 多重比较校正（effort #2058，R59）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3217–T3218，impl 1609）。
> 借鉴：Bonferroni / Holm-Bonferroni——族错误率（FWER）控制。

## Problem Statement

一次评估同时看 m 个指标的 p 值（A/B 多指标对比）：α=0.05 下每指标
5% 假阳性——m=20 指标期望 1 个「显灵」（多重比较陷阱），无校正的
「显著」不可信。

## Solution

`MultipleComparisonCorrection`（core/eval，纯函数零状态）：

- `bonferroni(pValues, alpha)`：pᵢ×m ≤ α 即显著——最保守单步；
- `holm(pValues, alpha)`：p 升序逐步比 α/(m−j+1)，**首次不显著即止**
 （其后全不显著——单调停步）——FWER 同控而功效恒不弱于
  Bonferroni；
- `Verdict`（索引序显著布尔 + 计数 + 方法名）；m=1 两法退化原始
  α 口径；
- 契约：p ∈ [0,1] 非 NaN、alpha ∈ (0,1)、非空 fail-fast。

## User Stories

1. 作为 A/B 作者，多指标显著的族错误率受控——「显灵」指标不再单独
   可信。
2. 作为评审，Holm 计数 ≥ Bonferroni 恒成立——功效免费午餐的口径。

## Testing Decisions

- Bonferroni p≤0.01 界（m=5）；Holm 阈值序列逐步 + 停步（乱序入参
  索引序断言）；Holm ⊇ Bonferroni 逐索引含 portraits；全大 p 零显著；
  m=1 退化原始口径；畸形六型 fail-fast。

## Out of Scope

- 不做 FDR（Benjamini-Hochberg——更松口径留白）；不做 p 值计算（输
  入口径）。

## Further Notes

- 与卡方/bootstrap（2048/903）成评估统计三件：判定/区间/多重度。
