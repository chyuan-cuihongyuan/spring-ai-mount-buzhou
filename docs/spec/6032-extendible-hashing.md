# Spec 6032 — Extendible Hashing 可扩目录哈希（effort #6032，T33）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6265–T6266，impl 2233）。
> 借鉴：Fagin 可扩目录哈希思想。

## Problem Statement

哈希表扩容的病：满即全量重哈希（扩容停顿放大）——**目录
按需翻倍+局部桶分裂面**缺失。

## Solution

`ExtendibleHashing`（core/metrics，源码已预载）：

- 键按哈希低 globalDepth 位寻址目录项；桶容量 4 溢出时：
  localDepth==globalDepth 先翻倍目录（其余项平移复制），
  再按 localDepth+1 重散布分裂该桶；
- 集合语义（重复幂等）；SplitMix64 混淆（确定性分布）；
- 读数：size/directorySize/globalDepth/bucketCount；
- fail-fast：无（集合语义全容）。

## User Stories

1. 作为存储作者，扩容只分裂单桶——无全量重哈希停顿。
2. 作为审计作者，globalDepth/bucketCount 显形——结构可查。

## Testing Decisions

- 300 键（7 步长）全命中+缺席不在；重复插入幂等；目录
  深度单调不减且翻倍触发；初始形态 1 深度 2 目录项钉住。

## Out of Scope

- 不做删除/收缩（增长面）；不做并发。

## Further Notes

- 与 RobinHoodHashTable（5045）同族不同面：开放寻址均衡
  探测 vs 目录翻倍桶分裂。
- 里程碑：T33/50（66%）。
