# Spec 3004 — top-p 核采样（effort #3004，R5）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5009–T5010，impl 2005）。
> 借鉴：nucleus sampling（Holtzman et al.，GPT-2 采样）。

## Problem Statement

温度/top-k 控制生成随机性各有盲区：top-k 硬截固定个数——尖峰分布
截太宽（长尾噪声混入）、平坦分布截太窄（合法多样性被砍）；贪心
 top-1 又完全丢失多样性。需要一个**按累积质量自适应**的截断口径。

## Solution

`NucleusSampler`（core/policy，纯函数 + RandomGenerator 注入）：

- `sample(logits, p, rng)`：softmax（max 减稳定化）→ 概率降序
  （同值小索引先——确定性）→ **累积质量 ≥ p 的最小前缀**为核 →
  核内重归一抽取；p→0 退化 top-1，p=1 全分布（连续插值两极端）；
- `keptCount(logits, p)` 核大小确定性读数（验证/对账面）；
- −∞ logit 质量为零永不中（禁选免掩码）；logits 空 / p∉(0,1] /
  全 −∞ fail-fast。

## User Stories

1. 作为路由作者，多模型/多技能按 logits 自适应截断——尖峰窄核、
   平坦宽核，不一刀切。
2. 作为评估作者，keptCount 核大小可读可断——截断口径可对账。

## Testing Decisions

- ln({0.5,0.3,0.15,0.05}) 阶梯手算（p=0.1/0.5→1、0.6→2、0.9/0.95→3、
  1.0→4）；p→0 千次恒 argmax；p=0.6 经验频率 0.625/0.375（种子
  回放 ±0.02）且核外恒零；p=1 有限项全覆盖 + −∞ 恒不中；同种子
  百次序列全等；非法参数五路 fail-fast。

## Out of Scope

- 不做温度缩放（与 top-k/温度正交，组合归调用方）；不做流式分桶
  采样；不接模型路由（接线归后续轮）。

## Further Notes

- 与 GumbelMaxSampler（免归一化单抽）/ WeightedSample（无放回
  k 抽）成采样三件：全分布单抽 / 加权无放回 / 核截断单抽。
- 里程碑：5/150。
