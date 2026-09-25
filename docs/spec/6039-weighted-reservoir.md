# Spec 6039 — Weighted Reservoir Sampler 加权蓄水池采样（effort #6039，T39）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6277–T6278，impl 2239）。
> 借鉴：A-Chao 加权蓄水池思想。

## Problem Statement

流式加权抽样的病：先收集全部再抽（内存随流线性爆）——
**单遍按权重入样面**缺失。

## Solution

`WeightedReservoirSampler`（core/policy，源码已预载）：

- A-Chao：前 m 项直接入池；其后第 i 项以 w_i/sumW 概率
  替换池中均匀随机一项（替换成功才累计 sumWeight）；
- 种子化 Random（同种子同流同样本——确定性可回放）；
- 读数：sample/size/seenCount/capacity；fail-fast：
  capacity≤0、weight≤0。

## User Stories

1. 作为采样作者，错误样本按量级加权留存——重错误必采样。
2. 作为回放作者，同种子同样本——抽样确定性可审计。

## Testing Decisions

- 500 项池 10 容量守恒；百万权重项驻留池中；同种子双实例
  样本全等；欠容量全保留；fail-fast。

## Out of Scope

- 不做无放回精确加权（A-ES 变体）；不做动态调权。

## Further Notes

- 与 ReservoirSample（observability）同族不同面：等权均匀
  vs 权重入样概率。
- 里程碑：T39/50（78%）。
