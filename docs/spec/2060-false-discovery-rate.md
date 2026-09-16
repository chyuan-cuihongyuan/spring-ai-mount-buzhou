# Spec 2060 — Benjamini-Hochberg FDR 校正（effort #2060，R61）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3221–T3222，impl 1611）。
> 借鉴：BH 程序——假发现率（FDR）控制（spec 2058 留白的更松口径）。

## Problem Statement

Holm（FWER——任一假阳性概率 ≤α）对探索性评估过紧：找候选指标/找
有效应子集要「宁多勿漏」——该控制的是显著集合中假阳性的**期望比例**
（FDR）而非任一假阳性概率，功效更高。

## Solution

`FalseDiscoveryRate`（core/eval，纯函数零状态）：

- `benjaminiHochberg(pValues, q)`：p 升序找**最大 j** 使 p₍ⱼ₎ ≤
  (j/m)·q——前 j 个全显著、其余不显著（截止序语义——孤立小 p 不
  能独立显著，与逐个判定区别）；
- q 默认 0.05（显著集合期望假阳性占比 ≤5%）；m=1 退化原始口径；
- 契约：p ∈ [0,1] 非 NaN、q ∈ (0,1)、非空 fail-fast。

## User Stories

1. 作为探索性评估作者，BH 高功效筛候选——显著集合含 ≤5% 假阳性
   的预期可控。
2. 作为评审，与 Holm 配对选口径：确证用 FWER（Holm）、探索用 FDR
   （BH）——一紧一松不再混用。

## Testing Decisions

- 截止序（j=2 截止恰两显）；孤立小 p 不救援（截止后大 p 中一个 0.01
  仅自身显著）；BH(4) > Holm(1) 功效对比（阈值序列手算）；全大 p 零；
  m=1 退化；畸形六型 fail-fast。

## Out of Scope

- 不做 q 值输出（adjusted p 留白）；不做加权 BH。

## Further Notes

- 评估统计族四件齐：卡方/bootstrap/Holm-FWER/BH-FDR。
