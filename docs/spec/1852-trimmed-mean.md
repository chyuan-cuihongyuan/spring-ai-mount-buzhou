# Spec 1852 — 截尾均值（effort #1852，R53）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2905–T2906，impl 1453）。借鉴：
> 统计学 trimmed mean / 体育评审惯例（去最高最低再平均）——离群值不污染
> 中心趋向，且不似中位数完全丢序信息。

## Problem Statement

均值被离群值劫持（一次 30s 卡顿把 100ms 中枢拉到 5s）、中位数又对序信息
 免疫过头（分布形状变化无感）——「去头尾 p% 再平均」的稳健中枢缺基建，
 评分清洗与延迟汇报各手搓各的。

## Solution

`TrimmedMean`（core/eval，静态纯函数）：

- `mean(samples, trimFraction)`：排序后双侧各截 ⌊n×f⌋ 再均；f ∈ [0, 0.5)
 （过半无中心语义 fail-fast）；
- 零截退化算术均；截后空 -1 哨兵；样本 null/NaN fail-fast。

## User Stories

1. 作为延迟汇报者，f=1/6 截尾后 30s 卡顿不再把中枢从 100 拉到 5s——
   离群值另有 P99 口径管。
2. 作为评审编排者，评委拉分（去最高最低）与延迟中枢同形状共用一件基建。
3. 作为框架宿主，截尾比例自声明，纯函数确定性可回放。

## Implementation Decisions

- 纯函数（排序-截尾-平均三步）；⌊⌋ 取整口径显式（n=6, f=1/6 → 截 1）。

## Testing Decisions

- 离群剔除（30s 不污染）；零截退化+对称截尾；截空/空表/null 哨兵；畸形
  四型 fail-fast。

## Out of Scope

- 不做 Winsorization（封顶替代截除归未来静脉）；不接指标导出。

## Further Notes

- 与 EvalScoreMad 互补：MAD 答离群多远，截尾均值答中枢多少。
