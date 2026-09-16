# Spec 2049 — Gumbel-max 采样器（effort #2049，R50）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3199–T3200，impl 1600）。
> 借鉴：Gumbel-max trick——按 logits 直接采样无需归一化。

## Problem Statement

按分数概率采样（路由探索 / 评估多样化）常规做法：softmax 归一化 →
累计分布 → 均匀逆采样——归一化有溢出/精度坑，三步流程长；禁选候选
（概率 0）需额外掩码逻辑。

## Solution

`GumbelMaxSampler`（core/policy，RandomGenerator 注入确定性可回放）：

- `sampleIndex(logits)`：argmax(logitsᵢ + Gumbelᵢ)，Gumbel = −ln(−ln(u))
 （u ∈ (0,1) 拒绝采样边界）——**数学等价 softmax(logits) 采样而免归
  一化**；-∞ logit 永不中（禁选免掩码——有限噪声加 -∞ 仍 -∞）；
- `empiricalFrequencies(logits, trials)`：经验频率蒙特卡洛读数——
  softmax 概率对账（测试与调参共用）；
- 契约：logits 非空非 NaN、random 非 null、trials ≥ 1 fail-fast。

## User Stories

1. 作为路由作者，logits 直接采样——禁选 -∞ 即零概率，无归一化溢出。
2. 作为评估作者，同种 RandomGenerator 全序列回放——采样可复现。

## Testing Decisions

- 同种双采样器 20 步全同（回放）；强偏 logits 主位 >900/1000；-∞
  禁选 500 次永不中；均匀 logits 四桶各 ∈[20%,30%]；softmax(ln2,0)=
  (2/3,1/3) 频率对账 ±0.04；畸形五型 fail-fast。

## Out of Scope

- 不做温度参数（调用方先缩放 logits）；不做批采样（循环调 sample）。

## Further Notes

- 与 Ucb1Selector（spec 2032）互补：确定性置信上界 vs 随机化探索。
