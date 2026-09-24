# Spec 5045 — Robin Hood Hash Table 劫富济贫哈希表（effort #5045，S46）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6191–T6192，impl 2196）。
> 借鉴：Robin Hood 开放寻址（探测距离均衡思想）。

## Problem Statement

哈希表的病：线性探测聚类越长越长（命中距离无上界、
方差失控）或墓碑堆积（删除后探测链腐化）——**探测
距离均衡+无墓碑删除面**缺失。

## Solution

`RobinHoodHashTable<K,V>`（core/metrics）：

- 插入线性探测，来键探测距离超过在位键即**换位**
 （富者让位贫者，被换键携其距离继续探测）——距离方差
  收窄、长簇截断；
- 删除**后向搬移**回填空洞（空洞/探测双索引：元素回家路
  过空洞即前移），无墓碑；
- 负载超 0.75 倍容重排（确定性增长史 resizeCount）；
- 读数：size/capacity/resizeCount/maxProbeDistance
 （均衡成效可见）；
- fail-fast：capacity<1、null 键值。

## User Stories

1. 作为存储作者，探测距离有界——最坏查询不失控。
2. 作为审计作者，maxProbeDistance 读数——聚类健康可见。

## Testing Decisions

- 构造性换位场景钉住（cap=8 插 7,15,0,16,17,24 跨环绕
  簇：maxProbeDistance=3、capacity 不扩、六键全可查）；
  删 15/7 后向搬移余键全可查；100 键 upsert（半数覆盖）
  +隔一删除回归；fail-fast。

## Out of Scope

- 不做有序遍历（哈希无序面）；不做并发（确定性单线程
  语义面）；不做自定义散列注入。

## Further Notes

- 与 MemTable（spec 5024）同族不同面：哈希无序 O(1) vs
  有序 O(log n)。Wave 8 第四件。
- 里程碑：S46/50（92%）。
