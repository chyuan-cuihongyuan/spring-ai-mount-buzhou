# Spec 4016 — HNSW 贪心层搜索（effort #4016，R17）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6033–T6034，impl 2117）。
> 借鉴：Malkov-Yashunin 2016；hnswlib/faiss 同款。

## Problem Statement

语义记忆/去重检索的 kNN 暴力全扫 O(n·d)——**对数级跳数换近似
召回**的分层图近邻件缺失。

## Solution

`HnswBeamSearch`（core/memory，欧氏距离）：

- 节点按指数分布随机落层（−ln(u)/ln(M)——高层稀疏长边跳大局）；
- 插入：高层贪心下降 + 落层内 beam（efConstruction 候选池）连
  M 近邻双向边（对端超限按距离裁远端）；
- 查询：顶降贪心到第 1 层，第 0 层 beam（ef=max(k,M)）——比纯
  贪心不困局部最优；k 超规模返全量；
- synchronized 单面；重复 id/维度不一致 fail-fast。

## User Stories

1. 作为记忆作者，稠密向量近邻检索免全扫——增量插入即查即用。
2. 作为去重作者，近重复检测的召回/成本杆（ef）可调。

## Testing Decisions

- 10×10 网格（无并列）四探针 top1 精确 + top5 与暴力基准集合相等；
  分批增量插入召回保持 + maxLevel 读数；k 超规模返 25 全量；
  空索引空返 + 畸形七型 fail-fast（null/k0/维度/重复 id/构造参）。

## Out of Scope

- 不做邻居选择启发式（启发式 pruning——朴素 M 近邻版）；不做
  删除/更新（归重建）；不做余弦/内积（欧氏口径）。

## Further Notes

- 与 MinHashSketch 互补（稠密向量 vs 集合相似度）。Wave 3
 （存储引擎族）收口。
- 里程碑：17/50。
