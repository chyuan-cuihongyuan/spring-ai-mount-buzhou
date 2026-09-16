# Spec 2016 — 布谷鸟过滤器（effort #2016，R17）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3133–T3134，impl 1567）。
> 借鉴：Cuckoo filter——指纹+双桶+踢出重排，近似成员**可删除**。

## Problem Statement

会话检疫/成员粗筛已有布伦（SessionBloomFilter），但布伦不能删：释放
成员后位图残留——误留永久假阳性，检疫集只增不减。需要「近似成员 +
可撤销」的过滤器。

## Solution

`CuckooFilter`（core/session，synchronized 小临界区）：

- 结构：桶数组（每桶 4 槽，16bit 指纹，0 哨兵避开）；元素 → 指纹
  fp 与双桶 i1/i2（i2 = i1 ^ hash(fp)——异或自定位）；
- `insert`：双桶空槽直入；双满踢出重排（**轮流起点确定性**——无
  随机，同序列同答案可回放；MAX_KICKS=500 踢尽拒插 false 并计
  overflowed——负载超限信号）；
- `mightContain`：任一候选桶含指纹即 true（可能假阳性，**不假
  阴性**）；
- `delete`：删一个指纹槽（近似——只删确曾插入的，调用方自律）；
  未见指纹 false；
- 读数：size（占用槽）/ overflowCount（扩容信号）；
- 契约：bucketCount 为 ≥16 的 2 的幂、item 非空 fail-fast。

## User Stories

1. 作为检疫作者，释放成员可撤销——检疫集不再只增不减。
2. 作为容量观测者，overflowCount > 0 即负载超限——扩容有据。

## Testing Decisions

- 插入/查询/删除闭环（删除后离场、旁员不动）；未知删除 false；删
  后可重插（释放后重新检疫）；万级非成员假阳性 <3%；小桶 200 灌入
  必溢出拒插+计数；同序列双实例行为全同（确定性踢出）；畸形五型
  fail-fast。

## Out of Scope

- 不做动态扩容（溢出即拒插——扩容策略归调用方）；不做计数变体
  （重复 insert 占多槽，调用方先 delete 可免）。

## Further Notes

- 与 SessionBloomFilter 互补：布伦查「见过吗」（不可撤），布谷鸟
  「还在吗」（可撤）。
