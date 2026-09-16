# Spec 2052 — 文本编辑距离（effort #2052，R53）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3205–T3206，impl 1603）。
> 借鉴：Levenshtein 经典 DP——文本差异精确口径。

## Problem Statement

文本相似判断散落内联实现（ConfigDoctor 私有 levenshtein 用于键纠错）
——配置键纠错 / 评估答案对比 / 技能名容错匹配各自造轮，口径不一；
近似口径（SimHash）不适用短文本与精确阈值场景。

## Solution

`TextDistance`（core/metrics，纯函数零状态）：

- `levenshtein(a, b)`：插/删/改各计 1 的最小编辑数——两行 DP 滚动
  数组 O(min(m,n)) 空间；空串口径（一方空=他方长度）；
- `similarity(a, b)` ∈ [0,1]：1 − dist/maxLen（双空=1 全同）——跨长
  短文本可比；
- `isNearMatch(a, b, threshold)`：相似比 ≥ 阈值（默认口径 0.8 与
  config 纠错同款）；契约：a/b 非 null、threshold ∈ [0,1] fail-fast。

## User Stories

1. 作为键纠错作者，「did you mean」候选按编辑距离统一口径——内联
   实现可收敛（ConfigDoctor 后续迁移）。
2. 作为评估作者，答案近似匹配精确阈值——不受 SimHash 短文本噪声。

## Testing Decisions

- 经典教材用例（kitten/sitting=3、flaw/lawn=2、intention/execution=5）；
  全同/双空/单向空；对称性；全改=maxLen 相似比 0；归一化（dist1/len5
  =0.8）；阈值判定（键纠错例 + 零阈全过/满阈全同）；500 长串；畸形
  五型 fail-fast。

## Out of Scope

- 不做 Damerau-Levenshtein（转置编辑留白）；不做 Jaro-Winkler 前缀
  加权变体；ConfigDoctor 迁移归后续轮。

## Further Notes

- 与 SimHash（spec 2038）互补：长文本近似筛查 vs 短文本精确阈值。
