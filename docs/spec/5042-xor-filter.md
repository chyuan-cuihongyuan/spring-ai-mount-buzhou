# Spec 5042 — Xor Filter 异或过滤器（effort #5042，S43）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6185–T6186，impl 2193）。
> 借鉴：Graf-Lemire xor filter（ClickHouse/DuckDB 社区，Bloom 后继）。

## Problem Statement

静态成员判定的病：Bloom 位阵（内存 ~10+ bits/键、查询
k 次探针）或精确集合（内存随键数线性爆）——**紧凑指纹
+ 单次查询面**缺失。

## Solution

`XorFilter`（core/metrics，静态键集语义）：

- 构建槽位数组（~1.23×键数，3 的倍数）；每键三独立散列
  （SplitMix64 盐变体）定位三槽+8 位指纹；
- **剥洋葱**构建：反复摘除度为一的槽-键边（逆序回填
  `array[slot]=fp^array[o1]^array[o2]`），失败换盐重试
  （确定性盐序列，≤64 轮）；连续失败 ISE（键集病态）；
- 查询=三槽异或等于键指纹——成员恒真（无假阴性）、
  假阳性 ≤ 2^-8 量级；单次数组访问级；
- 读数：checksum（同键集同校验和——构建确定性可审计）/
  arrayLength/elementCount；
- fail-fast：null 键/集、空集、重复键。

## User Stories

1. 作为去重作者，~9.84 bits/键——比 Bloom 更省且查询更少。
2. 作为审计作者，checksum 读数——同键集同构建可对账。

## Testing Decisions

- 1000 成员（i×7919）全命中（无假阴性）；arrayLength
  ≥1.23×键数且 3 的倍数；1 万非成员探针假阳性率 <10%
  （理论 ~2.6%）；同键集两次构建 checksum/arrayLength
  全等；5 键小集成员全命中；null/空/重复 fail-fast。

## Out of Scope

- 不做动态增删（静态集语义——Bloom/Cuckoo 已覆盖动态面）；
  不做 16/32 位指纹变体；不做序列化。

## Further Notes

- 与 SessionBloomFilter/CuckooFilter（session）同族不同面：
  异或三槽静态集 vs 布尔位阵/布谷鸟指纹动态集。Wave 8
  第一件。
- 里程碑：S43/50（86%）。
