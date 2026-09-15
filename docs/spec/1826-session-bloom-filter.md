# Spec 1826 — 会话布隆粗筛（effort #1826，R27）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2853–T2854，impl 1427）。借鉴：
> Bloom filter——「从未见过」的确定性快速判定：零假阴性、小概率假阳性、
> 内存代价是位图而非全集。

## Problem Statement

新会话到达时的归属判定（本实例见过没有）要查全量会话索引——索引本身
就是为全量查询设计的重结构；「**一定没见过**」这个最常见的分支（新会话
直达新建路径）其实只需要一个位图级粗筛。

## Solution

`SessionBloomFilter`（core/session，synchronized 小临界区）：

- `add(sessionId)` 幂等加入；`mightContain(sessionId)` 粗筛——**false 即
  一定没见过**（零假阴性契约），true 可能见过（小概率误报）；
- 确定性哈希（seed 混合 + FNV 式扩散，无随机数——可回放）；
- `fillRatio()` 饱和度（超 SATURATION_THRESHOLD=0.5 建议重建——布隆只增
  不能清）；
- 默认 4096 位 × 3 哈希（DEFAULT_BITS/DEFAULT_HASHES 常量）；契约 bits ≥ 64、
  hashes ∈ [1,8] fail-fast。

## User Stories

1. 作为路由层，粗筛 false → 直达新建路径，零索引查询——新会话快路径。
2. 作为内存治理者，位图 512B 覆盖万级会话的「见过」判定，替代一份索引
   副本。
3. 作为运维者，fillRatio 超 0.5 → 重建窗口到了（误报率开始爬升）。

## Implementation Decisions

- synchronized 小临界区（位图翻转与读数原子）；确定性 > 随机性（可回放
  可审计）。
- fail-fast：空白 id（两侧）、bits/hashes 越界。

## Testing Decisions

- 零假阴性（100 加入全报见过）；空布隆全拒 + 千探针误报 <5% + 饱和度界；
  跨实例确定性；幂等加入不抬饱和度；畸形四型 fail-fast。

## Out of Scope

- 不做计数布隆/可删除变体（Cuckoo/Counting 归未来静脉）；不接路由热路径。

## Further Notes

- 与 InMemorySessionIndex 正交：索引答「是哪个」，布隆只答「有没有」。
