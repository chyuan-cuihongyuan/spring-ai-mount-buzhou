# Spec 3010 — MinHash Jaccard 素描（effort #3010，R11）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5021–T5022，impl 2011）。
> 借鉴：MinHash（Broder 1997，AltaVista 近重复检测）。

## Problem Statement

近重复判定（数据集条目/会话轨迹/技能文档）全量两两精确 Jaccard
是 O(n²·|元素|)——万级条目即不可行；需要可预计算的固定长度签名
 *粗筛* 候选对。

## Solution

`MinHashSketch`（core/metrics）：

- k 路最小哈希签名（hashCount ≥1）：`offer(element)` 每元素 k 路
  派生哈希取最小（重复 offer 幂等——集合语义）；
- `similarityTo(other)` 相等位占比 = Jaccard 无偏估计
  （σ≈√(J(1−J)/k)）；交换律成立；
- 基散列复用 DeterministicHash.hash64，第 i 路 = Weyl 掺 index +
  splitmix64 终结器（**确定性**——同集合同签名，无随机源）；
- 空集合诚实：双空 NaN / 单空 0；路数不一致 fail-fast；
  signature() 防御性拷贝。

## User Stories

1. 作为数据质量作者，万级条目先签名粗筛候选对、再精确复核——
   两层口径替代 O(n²) 全量。
2. 作为对账作者，确定性签名可复算可审计（无随机源）。

## Testing Decisions

- 同集合（乱序 offer）估计恰 1.0；无交集恰 0.0；J=1/3 构造集
  k=256 估计 1/3±0.10；重复 offer 幂等（offerCount 5 vs 相似度
  1.0）；双空 NaN/单空 0；签名长度=路数+防御拷贝（外部改不动）；
  同元素跨实例签名相等；0/负/不一致路数 fail-fast。

## Out of Scope

- 不做 shingling 分片（n-gram 供给归 NgramExtractor）；不做 LSH
  分桶（band 技术留白——候选对检索归后续轮）；不做加权 MinHash。

## Further Notes

- 与 SimHashFingerprint（全指纹 Hamming）/ TextDistance（精确
  距离）互补：三件近似检索地基。
- 里程碑：11/150。
