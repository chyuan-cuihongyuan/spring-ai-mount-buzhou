# Spec 2051 — 加权无放回抽样（effort #2051，R52）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3203–T3204，impl 1602）。
> 借鉴：Efraimidis-Spirakis A-Res 加权水库——单遍无放回加权抽样。

## Problem Statement

按权重无放回抽 k 个（评估分层采样 / 实验组分配 / 金丝雀候选挑选）：
朴素做法逐次「按剩余权重轮盘 + 移除」是 O(nk)；放回式轮盘 k 次会
重复采样。

## Solution

`WeightedSample`（core/eval，纯函数，RandomGenerator 注入）：

- `sample(candidates, k, random)`：每元素 key = u^(1/w)（u ∈ (0,1)
  拒绝边界），key 前 k 大即样本——**单遍 O(n log k) 数学等价**逐次
  无放回轮盘；
- 权重 0 永不中；k ≥ 候选数 = 全取（按 key 序）；k=0 空；
- 契约：candidates/random 非 null、权重 ≥ 0 非 NaN、k ≥ 0 fail-fast。

## User Stories

1. 作为评估作者，分层采样一遍成——权重即层大小，无放回不重复。
2. 作为实验作者，同种 RandomGenerator 回放——分组可复现。

## Testing Decisions

- 50 轮 3/5 无重复；零权 100 轮永不中；k=10 超池返回全合格（零权除
  外）；k=0 空；同种 42 双跑同序列；100:1 权重千轮 >950 主导；畸形
  五型 fail-fast。

## Out of Scope

- 不做有放回口径（多调单抽）；不做最小方差（随机口径）。

## Further Notes

- 与 Gumbel-max（spec 2049）同族（key 化随机）互补：单次索引采样 vs
  无放回 k 样本。
