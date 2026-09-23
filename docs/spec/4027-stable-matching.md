# Spec 4027 — 稳定匹配（effort #4027，R28）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6055–T6056，impl 2128）。
> 借鉴：Gale-Shapley 1962（NRMP 实配算法）。

## Problem Statement

双边偏好对接（模型-路由/agent-槽位）贪婪逐最大会产出**阻塞对**
（双方私下都更愿意换）——稳定性语义件缺失。

## Solution

`StableMatching`（core/policy，纯静态）：

- 延迟接受：求婚方按序出价；受婚方持有当前最优（按其偏好序）
  并拒绝余者，被拒者继续下一选；有限步收敛；
- 结果**稳定**（无阻塞对）且对求婚方最优（主动方优势）；
- 自由队列按入参序（确定性可回放）；不齐边与链尽者诚实不配
 （缺席于结果）。

## User Stories

1. 作为对接作者，双边偏好的产物无阻塞对——无私下换机激励。
2. 作为审计者，稳定性可机检（阻塞对全扫描零命中）。

## Testing Decisions

- 冲突换优（A 先占 x、x 偏好 B 踢 A、A 转 y）；双方首选直配；
  不齐边 z 闲置；链尽者缺席；3×3 全序偏好稳定性全扫描零阻塞
  对；畸形五型 fail-fast（Map.of 拒 null 值的测试构造改 HashMap）。

## Out of Scope

- 不做加权/带容量匹配（医院-居民多容量 HR 变体）；不做最优性
  证明面（只验稳定性与确定性）；不做策略性虚报分析。

## Further Notes

- 与 TopologicalSorter（偏序排程）同族不同问：拓扑管顺序、
  本件管配对。
- 里程碑：28/50。
