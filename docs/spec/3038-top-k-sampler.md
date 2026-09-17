# Spec 3038 — top-k + 温度采样（effort #3038，R39）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5077–T5078，impl 2038）。
> 借鉴：LLM 采样双旋钮（top-k 截断 × temperature 缩放）。

## Problem Statement

路由/生成的随机性控制需要「候选集宽度」与「集内锐度」两个正交
旋钮：top-p 只有自适应宽度、temperature 只有锐度——组合口径缺
显式件。

## Solution

`TopKSampler`（core/policy，纯函数 + 注入）：

- `sample(logits, k, T, rng)`：前 k 名截断（logit 降序、同值小
  索引先——确定性）→ (l−max)/T 稳定 softmax → 归一抽取；
- 四象限：k=1 恒 argmax / T→0 趋贪心 / T 大趋均匀 / k 截断集外
  恒零；−∞ 零概率永不中；
- `probabilities(...)` 公共读数（对账面）；logits 空 / k∉[1,len] /
  T ≤ 0 fail-fast。

## User Stories

1. 作为路由作者，候选宽度与冷热锐度独立调——宽窄冷热四象限。
2. 作为对账作者，概率读数可复算——采样口径可审计。

## Testing Decisions

- k=1 千抽恒 argmax；T=0.01 五千抽 >98% argmax；T=10000 三万抽
  三项各 1/3±0.02；读数手算 {ln4,0}→{0.8,0.2}；截断集外恰零；
  −∞ 两千抽永不中；同种子回放；四路 fail-fast。

## Out of Scope

- 不做 top-k 与 top-p 混合截断（组合归调用方）；不做 min-p；
  不做惩罚项（repetition penalty 另件留白）。

## Further Notes

- 与 NucleusSampler（自适应宽度）成对：k 固定宽度、p 自适应
  宽度——各有盲区互补。
- 里程碑：39/150。
