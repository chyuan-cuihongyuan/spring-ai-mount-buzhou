# Spec 1894 — 尾时延放大读面（effort #1894，R95）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2989–T2990，impl 1495）。借鉴：
> Google "The Tail at Scale" / HN 经典——请求触达 N 个盒子，单盒
> 99 分位好不算好：端到端全好概率 = q^N，q=0.99、N=100 → 仅 36.6%
——尾时延被并行度幂次放大。

## Problem Statement

微服务分位报告各自 99 分位全绿、端到端却大面积慢——「分位好了
就行」的直觉在并行扇出下失效：放大倍数是幂函数不是线性，缺独立
计算面时容量与 SLO 分解全错。

## Solution

`TailAmplification`（core/metrics，静态纯函数）：

- `endToEndProbability(perBoxQuantile, boxCount)`：q^N——请求触达
  全部盒子均在分位内的概率（独立性假设）；
- `requiredPerBoxQuantile(endToEndTarget, boxCount)`：目标^(1/N)——
  SLO 反解单盒需要几 分位（0.99/100 盒 → 4 个 9）。

## User Stories

1. 作为 SLO 分解者，端到端 99% 目标 100 盒 → 单盒需 0.9999——
   每盒只做 99% 根本不够。
2. 作为架构评审者，q=0.99 N=100 → 端到端仅 36.6%——扇出宽度
   的代价有数。
3. 作为汇报者，两函数互逆可交叉验证——账面自洽可证。

## Implementation Decisions

- 纯函数零状态；独立性假设（诚实边界入档——相关失效会更差）；
  q/target ∈ (0,1]、boxCount ≥ 1 fail-fast。

## Testing Decisions

- 经典 0.99^100≈0.366；反解 0.99^(1/100)≈0.9999；N=1 恒等；互逆
  交叉验证；畸形三型 fail-fast。

## Out of Scope

- 不做相关失效建模；不做扇出拓扑优化。

## Further Notes

- 与 CriticalPathLength（CPM 下界）互补：那是确定性时长下界，
  这是概率性分位放大。
