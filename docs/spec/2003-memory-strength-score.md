# Spec 2003 — 记忆强度三分量评分（effort #2003，R4）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3107–T3108，impl 1554）。
> 借鉴：mem0——记忆分 = 加权(recency, frequency, importance)，检索
> 排序与衰减淘汰的统一排序键。

## Problem Statement

记忆条目的检索排序与淘汰缺乏统一强度口径：新近性、访问频次、显式重要
度各自为政，排序键因场景而异——同一批记忆在 recall 排序与压缩淘汰两处
口径不一致，行为不可解释。

## Solution

`MemoryStrengthScore`（buzhou-memory recall 包，纯函数零状态）：

- 三分量：recency = 2^(−Δt/halfLife)（半衰期衰减，Δt=半衰期时恰 0.5，
  默认 24h）；frequency = 1−1/(1+ln(1+count))（对数饱和——重访增益
  边际递减，永不达 1）；importance 输入钳制 [0,1] 直通；
- `Weights` record（recency/frequency/importance 各 ≥ 0 且不全零
  fail-fast，默认 0.5/0.3/0.2）；
- score = Σwᵢ·分量ᵢ / Σwᵢ ∈ [0,1]（权重归一——任意权重标度同答案）；
- 契约：Δt ≥ 0、count ≥ 0、halfLife > 0 fail-fast。

## User Stories

1. 作为 recall 排序作者，统一强度键排序——新近热访高重要记忆居前，
   口径可解释（三分量读数分解）。
2. 作为压缩淘汰作者，低分记忆优先淘汰——与 recall 同键，行为一致。
3. 作为调参者，Weights 三旋钮按场景偏置（客服场景抬 recency，知识库
   场景抬 importance）。

## Implementation Decisions

- 纯函数（无时钟注入——Δt 由调用方带入，测试确定性）；半衰期语义
  2^(−Δt/T)（Δt=T 恰 0.5，比 e 指数口径直观可调参）。

## Testing Decisions

- 半衰期三点（1/0.5/0.25 平方）；对数饱和边际递减（10k→1M 增益 <
  0→10 增益）；重要度钳制三态；Δt 单调性；输出域 [0,1]（含极值入参）；
  畸形六型 fail-fast。

## Out of Scope

- 不接存储/检索管线接线（排序键落地归后续轮）；
- 不做 importance 的 LLM 打分通道（输入直通，打分归上层）。

## Further Notes

- 与 LexicalDriftDetector/SemanticDriftDetector（压缩触发）正交：漂移
  定时机，强度定次序。
