# Spec 2025 — 一致性哈希环（effort #2025，R26）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3151–T3152，impl 1576）。
> 借鉴：Dynamo/Ketama——虚节点环 + 顺时针归属，节点增减最小迁移。

## Problem Statement

键→节点归属用朴素取模：节点增减时全量重映射（缓存全失效/会话全重
路由）；直接哈希节点又分布不均（单节点独占大弧段）。

## Solution

`ConsistentHashRing`（core/policy，synchronized 小临界区）：

- `addNode(node)`：铺 virtualNodes（默认 160，Ketama 惯例）个虚节点
  （node#i 散列分散弧段——物理节点在环上均匀）；`removeNode` 撤其
  全部虚节点；
- `nodeFor(key)`：环上 ≥ hash(key) 的首个虚节点之物理节点（回绕到
  首节点；空环 null）；
- 迁移最小性：删节点只影响其弧段上的键（迁移量 = 其原份额 ≈ 1/n，
  非全量）；加节点只吸收其弧段（新键必归新节点）；
- 读数：nodeCount / virtualNodeCount（环容量对账）；
- 契约：node 非空非重复、virtualNodes ≥ 1 fail-fast；确定性散列
  （FNV-1a 64 + splitmix64——与 HLL/频率素描同款）。

## User Stories

1. 作为黏性路由作者，节点增减只有 1/n 会话重路由——不全量抖动。
2. 作为容量观测者，分布份额均衡（虚节点打散）——无独占弧段。

## Testing Decisions

- 同键确定性；三节点 3 万键份额 ∈ [20%,47%]；删节点迁移量恰等于其
  原份额且变键原归属必是被删者；加节点吸收量 ∈ (0, 5000) 且新键必归
  新节点；空环 null；单虚节点回绕；畸形六型 fail-fast。

## Out of Scope

- 不做权重异构（每节点同 virtualNodes——加权环留白）；不接路由链
  （会话黏性接线归后续轮）。

## Further Notes

- 与会话黏性路由提示（#415）正交：那是指示器，这是归属计算器。
