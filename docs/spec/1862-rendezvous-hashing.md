# Spec 1862 — Rendezvous 哈希（effort #1862，R63）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2925–T2926，impl 1464）。借鉴：
> Highest Random Weight（HRW/Rendezvous）——键×节点打分取最高：节点
> 增删只影响原属它的 1/n 键，免一致性哈希环的虚节点与环管理。

## Problem Statement

分片归属（会话→实例/键→缓存节点）用取模哈希则节点增删全量重排（n 变
 一下全键迁移）；一致性哈希环要虚节点调均匀——「最小迁移 + 免环管理 +
 确定性」的第三选项缺基建。

## Solution

`RendezvousHashing`（core/cache，静态纯函数）：

- `assign(key, nodes)`：score = 确定性混合（key×node 黄金比扩散——布隆
  同口径），取最高；并列取字典序最小（稳定复现）；
- `assignAll(keys, nodes)` 全量指派（确定性可回放）；
- 最小迁移性：摘除节点 j 后，只有原本归属 j 的键换归属（其余键最高分
  获得者不变——数学性质）。

## User Stories

1. 作为分片作者，3→2 节点缩容只有 1/3 键迁移——其余键归属纹丝不动。
2. 作为审计者，无随机数同入参同归属——指派计划可回放比对。
3. 作为框架宿主，节点语义（实例/分片/缓存节点）自声明，纯指派不路由。

## Implementation Decisions

- 纯函数；并列字典序（并列在 64 位混合下罕见但确定性优先）；O(n) 每
  键（节点数十级合适，万级节点该用环——诚实边界入档）。

## Testing Decisions

- 确定性；摘除节点仅原属它的键迁移（200 键全检）；300 键 3 节点弱均匀
  （各 >50）；畸形四型 fail-fast。

## Out of Scope

- 不做加权节点（weighted HRW 归未来静脉）；不执行路由。

## Further Notes

- 与 SmoothWeightedSequence 正交：那是请求派发，这是键归属。
