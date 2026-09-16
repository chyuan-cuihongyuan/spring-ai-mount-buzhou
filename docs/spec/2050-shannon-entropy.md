# Spec 2050 — 香农熵读数（effort #2050，R51）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3201–T3202，impl 1601）。
> 借鉴：Shannon 1948 信息熵——分布多样性统一量纲。

## Problem Statement

分布多样性判断缺连续量纲：卡方答「是否偏斜」（二值），「偏多斜」
「比上次更多样」无从比较——答案多样性 / 路由集中度 / 工具分布需要
跨分布可比的统一度量。

## Solution

`ShannonEntropy`（core/metrics，纯函数零状态）：

- `entropy(counts, base)`：H = −Σp·log_b p（零频类不计——0·log0=0
  惯例）；base > 1 可选（2=bits 便捷 `entropyBits` / e=nats）；
- `normalizedEntropy(counts)` ∈ [0,1]：H(bits) ÷ log₂(k)，k=**非零**
  类数（零频类不抬上界）——全集中 0、均匀 1，跨分布直接可比；
- 契约：counts 非空非负、总量 > 0、base > 1 fail-fast。

## User Stories

1. 作为评估作者，答案分布归一化熵 0.6→0.8——多样性改善跨 run 可比。
2. 作为路由作者，熵连续下降 = 集中度恶化趋势——比卡方二值判定更早
   显形。

## Testing Decisions

- 全集中 0（含单类）；均匀四类恰 2 bits、归一化恰 1；零频类不影响
  熵与上界；(3,1) 手算 0.811278；nats/bits 换算 ln2；(7,3) 归一化 ∈
  (0,1)；畸形六型 fail-fast。

## Out of Scope

- 不做 Rényi/Tsallis 广义熵（α 参数族留白）；不接具体读面（归调用
  方）。

## Further Notes

- 与卡方（spec 2048）互补：熵连续量纲 vs 卡方二值判定；EvalCoverage
  Matrix 嵌套熵可后续迁移本件（收敛口径）。
