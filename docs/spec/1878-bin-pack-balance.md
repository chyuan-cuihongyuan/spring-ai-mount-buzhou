# Spec 1878 — 装箱平衡（effort #1878，R79）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2957–T2958，impl 1479）。借鉴：
> Kubernetes/Mesos（百 K 星级）bin-packing 调度语义——first-fit-decreasing
> 装箱：任务按体积降序、逐个塞进首个装得下的节点；装箱数与浪费率
> 事前可算，扩容/缩容决策有据。

## Problem Statement

批任务落节点两头拍脑袋：全排最优是 NP-hard 退缩化为「能塞就塞」，
节点数与碎片率无账——该开几个节点、缩容能省多少、倾斜是否值得
重排，缺一个确定性计算面。

## Solution

`BinPackBalance`（core/policy，静态纯函数 + 嵌套 PackResult）：

- `pack(itemSizes, binCapacity)`：first-fit-decreasing——降序排序
  （同体积保持原序，稳定），逐个放入首个剩余容量足够的箱，放不下
  开新箱；返回 binsUsed + 逐箱载荷（PackResult）；
- `wasteRatio(loads, binCapacity)`：1 − Σ载荷/(箱数×容量)——装箱
  浪费率（0 = 无碎片）。

## User Stories

1. 作为容量规划者，[4,3,3,2,2] 容量 6 → 3 箱、浪费 2/9——扩容
   预算与碎片一目了然。
2. 作为成本优化者，完美装（[3,3,3] 容量 9 → 1 箱 0 浪费）对照
   松散装——缩容收益可直接对比。
3. 作为确定性评审者，同体积保序——同输入同输出可回放。

## Implementation Decisions

- FFD 近似（最优装箱 NP-hard，FFD 保证 ≤ 11/9 OPT + 常数）；容量
  ≥ 1、体积 ≥ 0、空表即 0 箱 fail-fast/哨兵分明；纯计算不落位。

## Testing Decisions

- 经典 [4,3,3,2,2]/6 → 3 箱载荷 {6,6,2}；完美装 [3,3,3]/9 → 1 箱
  0 浪费；空表 0 箱；浪费率 2/9 精确断言；畸形三型（容量 0/负体积
  /空表 wasteRatio）fail-fast。

## Out of Scope

- 不做实际调度与迁移（归执行层）；不做最优装箱精确解（NP-hard）。

## Further Notes

- 与 TwoChoiceSelector（在线两随机取轻）互补：那是流量在线分，
  这是批任务离线装；与 MaglevHash/JumpConsistentHash（请求归属）
  互补：那是稳态映射，这是容量规划。
